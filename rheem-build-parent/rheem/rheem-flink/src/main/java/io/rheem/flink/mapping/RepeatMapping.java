package io.rheem.flink.mapping;

import io.rheem.basic.operators.RepeatOperator;
import io.rheem.core.mapping.Mapping;
import io.rheem.core.mapping.OperatorPattern;
import io.rheem.core.mapping.PlanTransformation;
import io.rheem.core.mapping.ReplacementSubplanFactory;
import io.rheem.core.mapping.SubplanPattern;
import io.rheem.core.types.DataSetType;
import io.rheem.flink.operators.FlinkRepeatExpandedOperator;
import io.rheem.flink.operators.FlinkRepeatOperator;
import io.rheem.flink.platform.FlinkPlatform;

import java.util.Collection;
import java.util.Collections;

/**
 * Mapping from {@link RepeatOperator} to {@link FlinkRepeatOperator}.
 */
@SuppressWarnings("unchecked")
public class RepeatMapping implements Mapping{
    @Override
    public Collection<PlanTransformation> getTransformations() {
        return Collections.singleton(new PlanTransformation(
                this.createSubplanPattern(),
                this.createReplacementSubplanFactory(),
                FlinkPlatform.getInstance()
        ));
    }

    private SubplanPattern createSubplanPattern() {
        final OperatorPattern operatorPattern = new OperatorPattern(
                "repeat", new RepeatOperator<>(1, DataSetType.none()), false
        );
        return SubplanPattern.createSingleton(operatorPattern);
    }


    private ReplacementSubplanFactory createReplacementSubplanFactory() {
        return new ReplacementSubplanFactory.OfSingleOperators<RepeatOperator>(
                (matchedOperator, epoch) -> new FlinkRepeatExpandedOperator<>(matchedOperator).at(epoch)
        );
    }
}
