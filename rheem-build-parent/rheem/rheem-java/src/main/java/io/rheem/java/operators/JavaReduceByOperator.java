package io.rheem.java.operators;

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
import io.rheem.java.channels.CollectionChannel;
import io.rheem.java.channels.JavaChannelInstance;
import io.rheem.java.channels.StreamChannel;
import io.rheem.java.execution.JavaExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Java implementation of the {@link ReduceByOperator}.
 */
public class JavaReduceByOperator<Type, KeyType>
        extends ReduceByOperator<Type, KeyType>
        implements JavaExecutionOperator {


    /**
     * Creates a new instance.
     *
     * @param type             type of the reduce elements (i.e., type of {@link #getInput()} and {@link #getOutput()})
     * @param keyDescriptor    describes how to extract the key from data units
     * @param reduceDescriptor describes the reduction to be performed on the elements
     */
    public JavaReduceByOperator(DataSetType<Type> type, TransformationDescriptor<Type, KeyType> keyDescriptor,
                                ReduceDescriptor<Type> reduceDescriptor) {
        super(keyDescriptor, reduceDescriptor, type);
    }

    /**
     * Copies an instance (exclusive of broadcasts).
     *
     * @param that that should be copied
     */
    public JavaReduceByOperator(ReduceByOperator<Type, KeyType> that) {
        super(that);
    }

    @Override
    public Tuple<Collection<ExecutionLineageNode>, Collection<ChannelInstance>> evaluate(
            ChannelInstance[] inputs,
            ChannelInstance[] outputs,
            JavaExecutor javaExecutor,
            OptimizationContext.OperatorContext operatorContext) {
        assert inputs.length == this.getNumInputs();
        assert outputs.length == this.getNumOutputs();

        final Function<Type, KeyType> keyExtractor = javaExecutor.getCompiler().compile(this.keyDescriptor);
        final BinaryOperator<Type> reduceFunction = javaExecutor.getCompiler().compile(this.reduceDescriptor);
        JavaExecutor.openFunction(this, reduceFunction, inputs, operatorContext);

        final Map<KeyType, Type> reductionResult = ((JavaChannelInstance) inputs[0]).<Type>provideStream().collect(
                Collectors.groupingBy(keyExtractor, new ReducingCollector<>(reduceFunction))
        );
        ((CollectionChannel.Instance) outputs[0]).accept(reductionResult.values());

        return ExecutionOperator.modelEagerExecution(inputs, outputs, operatorContext);
    }

    @Override
    public String getLoadProfileEstimatorConfigurationKey() {
        return "rheem.java.reduceby.load";
    }

    @Override
    public Optional<LoadProfileEstimator> createLoadProfileEstimator(Configuration configuration) {
        final Optional<LoadProfileEstimator> optEstimator =
                JavaExecutionOperator.super.createLoadProfileEstimator(configuration);
        LoadProfileEstimators.nestUdfEstimator(optEstimator, this.keyDescriptor, configuration);
        LoadProfileEstimators.nestUdfEstimator(optEstimator, this.reduceDescriptor, configuration);
        return optEstimator;
    }

    @Override
    protected ExecutionOperator createCopy() {
        return new JavaReduceByOperator<>(this.getType(), this.getKeyDescriptor(), this.getReduceDescriptor());
    }

    @Override
    public List<ChannelDescriptor> getSupportedInputChannels(int index) {
        assert index <= this.getNumInputs() || (index == 0 && this.getNumInputs() == 0);
        if (this.getInput(index).isBroadcast()) return Collections.singletonList(CollectionChannel.DESCRIPTOR);
        return Arrays.asList(CollectionChannel.DESCRIPTOR, StreamChannel.DESCRIPTOR);
    }

    @Override
    public List<ChannelDescriptor> getSupportedOutputChannels(int index) {
        assert index <= this.getNumOutputs() || (index == 0 && this.getNumOutputs() == 0);
        return Collections.singletonList(CollectionChannel.DESCRIPTOR);
    }

    /**
     * Immitates {@link Collectors#reducing(BinaryOperator)} but assumes that there is always at least one element
     * in each reduction.
     */
    private static class ReducingCollector<T> implements Collector<T, List<T>, T> {

        private final BinaryOperator<T> reduceFunction;

        ReducingCollector(BinaryOperator<T> reduceFunction) {
            this.reduceFunction = reduceFunction;
        }

        @Override
        public Supplier<List<T>> supplier() {
            return () -> new ArrayList<>(1);
        }

        @Override
        public BiConsumer<List<T>, T> accumulator() {
            return (list, element) -> {
                if (list.isEmpty()) {
                    list.add(element);
                } else {
                    list.set(0, this.reduceFunction.apply(list.get(0), element));
                }
            };
        }

        @Override
        public BinaryOperator<List<T>> combiner() {
            return (list1, list2) -> {
                if (list1.isEmpty()) {
                    return list2;
                } else if (list2.isEmpty()) {
                    return list2;
                } else {
                    list1.set(0, this.reduceFunction.apply(list1.get(0), list2.get(0)));
                    return list1;
                }
            };
        }

        @Override
        public Function<List<T>, T> finisher() {
            return list -> {
                assert !list.isEmpty();
                return list.get(0);
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}
