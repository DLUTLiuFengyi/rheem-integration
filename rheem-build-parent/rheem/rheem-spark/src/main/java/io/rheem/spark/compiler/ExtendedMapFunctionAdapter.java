package io.rheem.spark.compiler;

import org.apache.spark.api.java.function.Function;
import io.rheem.core.function.ExecutionContext;
import io.rheem.core.function.FunctionDescriptor;
import io.rheem.spark.execution.SparkExecutionContext;

/**
 * Implements a {@link Function} that calls {@link io.rheem.core.function.ExtendedFunction#open(ExecutionContext)}
 * of its implementation before delegating the very first {@link Function#call(Object)}.
 */
public class ExtendedMapFunctionAdapter<InputType, OutputType> implements Function<InputType, OutputType> {

    private final FunctionDescriptor.ExtendedSerializableFunction<InputType, OutputType> impl;

    private final SparkExecutionContext executionContext;

    private boolean isFirstRun = true;

    public ExtendedMapFunctionAdapter(FunctionDescriptor.ExtendedSerializableFunction<InputType, OutputType> extendedFunction,
                                      SparkExecutionContext sparkExecutionContext) {
        this.impl = extendedFunction;
        this.executionContext = sparkExecutionContext;
    }

    @Override
    public OutputType call(InputType dataQuantume) throws Exception {
        if (this.isFirstRun) {
            this.impl.open(this.executionContext);
            this.isFirstRun = false;
        }

        return this.impl.apply(dataQuantume);
    }

}
