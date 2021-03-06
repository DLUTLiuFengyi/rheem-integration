package io.rheem.spark.operators;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import io.rheem.basic.operators.ReduceByOperator;
import io.rheem.core.api.Configuration;
import io.rheem.core.function.ReduceDescriptor;
import io.rheem.core.function.TransformationDescriptor;
import io.rheem.core.optimizer.OptimizationContext;
import io.rheem.core.optimizer.costs.LoadProfileEstimator;
import io.rheem.core.optimizer.costs.LoadProfileEstimators;
import io.rheem.core.plan.rheemplan.ExecutionOperator;
import io.rheem.core.platform.ChannelDescriptor;
import io.rheem.core.platform.ChannelInstance;
import io.rheem.core.platform.lineage.ExecutionLineageNode;
import io.rheem.core.types.DataSetType;
import io.rheem.core.util.Tuple;
import io.rheem.spark.channels.BroadcastChannel;
import io.rheem.spark.channels.RddChannel;
import io.rheem.spark.execution.SparkExecutor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spark implementation of the {@link ReduceByOperator}.
 */
public class SparkReduceByOperator<Type, KeyType>
        extends ReduceByOperator<Type, KeyType>
        implements SparkExecutionOperator {


    /**
     * Creates a new instance.
     *
     * @param type             type of the reduce elements (i.e., type of {@link #getInput()} and {@link #getOutput()})
     * @param keyDescriptor    describes how to extract the key from data units
     * @param reduceDescriptor describes the reduction to be performed on the elements
     */
    public SparkReduceByOperator(DataSetType<Type> type, TransformationDescriptor<Type, KeyType> keyDescriptor,
                                 ReduceDescriptor<Type> reduceDescriptor) {
        super(keyDescriptor, reduceDescriptor, type);
    }

    /**
     * Copies an instance (exclusive of broadcasts).
     *
     * @param that that should be copied
     */
    public SparkReduceByOperator(ReduceByOperator<Type, KeyType> that) {
        super(that);
    }

    @Override
    public Tuple<Collection<ExecutionLineageNode>, Collection<ChannelInstance>> evaluate(
            ChannelInstance[] inputs,
            ChannelInstance[] outputs,
            SparkExecutor sparkExecutor,
            OptimizationContext.OperatorContext operatorContext) {
        assert inputs.length == this.getNumInputs();
        assert outputs.length == this.getNumOutputs();

        RddChannel.Instance input = (RddChannel.Instance) inputs[0];
        RddChannel.Instance output = (RddChannel.Instance) outputs[0];

        final JavaRDD<Type> inputStream = input.provideRdd();
        final PairFunction<Type, KeyType, Type> keyExtractor =
                sparkExecutor.getCompiler().compileToKeyExtractor(this.keyDescriptor);
        Function2<Type, Type, Type> reduceFunc =
                sparkExecutor.getCompiler().compile(this.reduceDescriptor, this, operatorContext, inputs);
        final JavaPairRDD<KeyType, Type> pairRdd = inputStream.mapToPair(keyExtractor);
        this.name(pairRdd);
        final JavaPairRDD<KeyType, Type> reducedPairRdd =
                pairRdd.reduceByKey(reduceFunc, sparkExecutor.getNumDefaultPartitions());
        this.name(reducedPairRdd);
        final JavaRDD<Type> outputRdd = reducedPairRdd.map(new TupleConverter<>());
        this.name(outputRdd);

        output.accept(outputRdd, sparkExecutor);

        return ExecutionOperator.modelLazyExecution(inputs, outputs, operatorContext);
    }

    @Override
    protected ExecutionOperator createCopy() {
        return new SparkReduceByOperator<>(this.getType(), this.getKeyDescriptor(), this.getReduceDescriptor());
    }

    /**
     * Extracts the value from a {@link scala.Tuple2}.
     * <p><i>TODO: See, if we can somehow dodge all this conversion, which is likely to happen a lot.</i></p>
     */
    private static class TupleConverter<InputType, KeyType>
            implements Function<scala.Tuple2<KeyType, InputType>, InputType> {

        @Override
        public InputType call(scala.Tuple2<KeyType, InputType> scalaTuple) throws Exception {
            return scalaTuple._2;
        }
    }

    @Override
    public String getLoadProfileEstimatorConfigurationKey() {
        return "rheem.spark.reduceby.load";
    }

    @Override
    public Optional<LoadProfileEstimator> createLoadProfileEstimator(Configuration configuration) {
        final Optional<LoadProfileEstimator> optEstimator =
                SparkExecutionOperator.super.createLoadProfileEstimator(configuration);
        LoadProfileEstimators.nestUdfEstimator(optEstimator, this.keyDescriptor, configuration);
        LoadProfileEstimators.nestUdfEstimator(optEstimator, this.reduceDescriptor, configuration);
        return optEstimator;
    }

    @Override
    public List<ChannelDescriptor> getSupportedInputChannels(int index) {
        if (index == 0) {
            return Arrays.asList(RddChannel.UNCACHED_DESCRIPTOR, RddChannel.CACHED_DESCRIPTOR);
        } else {
            return Collections.singletonList(BroadcastChannel.DESCRIPTOR);
        }
    }

    @Override
    public List<ChannelDescriptor> getSupportedOutputChannels(int index) {
        assert index <= this.getNumOutputs() || (index == 0 && this.getNumOutputs() == 0);
        return Collections.singletonList(RddChannel.UNCACHED_DESCRIPTOR);
    }

    @Override
    public boolean containsAction() {
        return false;
    }

}
