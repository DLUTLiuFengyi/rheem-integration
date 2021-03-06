package io.rheem.spark.operators;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import io.rheem.core.optimizer.OptimizationContext;
import io.rheem.core.plan.rheemplan.ExecutionOperator;
import io.rheem.core.platform.ChannelInstance;
import io.rheem.core.platform.lineage.ExecutionLineageNode;
import io.rheem.core.util.Tuple;
import io.rheem.spark.execution.SparkExecutor;
import io.rheem.spark.platform.SparkPlatform;

import java.util.Collection;

/**
 * Execution operator for the {@link SparkPlatform}.
 */
public interface SparkExecutionOperator extends ExecutionOperator {

    @Override
    default SparkPlatform getPlatform() {
        return SparkPlatform.getInstance();
    }

    /**
     * Evaluates this operator. Takes a set of {@link ChannelInstance}s according to the operator inputs and manipulates
     * a set of {@link ChannelInstance}s according to the operator outputs -- unless the operator is a sink, then it triggers
     * execution.
     * <p>In addition, this method should give feedback of what this instance was doing by wiring the
     * {@link io.rheem.core.platform.lineage.LazyExecutionLineageNode}s of input and ouput {@link ChannelInstance}s and
     * providing a {@link Collection} of executed {@link ExecutionLineageNode}s.</p>
     *
     * @param inputs          {@link ChannelInstance}s that satisfy the inputs of this operator
     * @param outputs         {@link ChannelInstance}s that accept the outputs of this operator
     * @param sparkExecutor   {@link SparkExecutor} that executes this instance
     * @param operatorContext optimization information for this instance
     * @return {@link Collection}s of what has been executed and produced
     */
    Tuple<Collection<ExecutionLineageNode>, Collection<ChannelInstance>> evaluate(
            ChannelInstance[] inputs,
            ChannelInstance[] outputs,
            SparkExecutor sparkExecutor,
            OptimizationContext.OperatorContext operatorContext);

    /**
     * Tell whether this instances is a Spark action. This is important to keep track on when Spark is actually
     * initialized.
     *
     * @return whether this instance issues Spark actions
     */
    boolean containsAction();

    /**
     * Utility method to name an RDD according to this instance's name.
     *
     * @param rdd that should be renamed
     * @see #getName()
     */
    default void name(JavaRDD<?> rdd) {
        if (this.getName() != null) {
            rdd.setName(this.getName());
        } else {
            rdd.setName(this.toString());
        }
    }

    /**
     * Utility method to name an RDD according to this instance's name.
     *
     * @param rdd that should be renamed
     * @see #getName()
     */
    default void name(JavaPairRDD<?, ?> rdd) {
        if (this.getName() != null) {
            rdd.setName(this.getName());
        } else {
            rdd.setName(this.toString());
        }
    }

}
