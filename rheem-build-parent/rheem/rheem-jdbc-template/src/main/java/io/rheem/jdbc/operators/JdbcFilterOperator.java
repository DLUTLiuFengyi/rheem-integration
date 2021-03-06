package io.rheem.jdbc.operators;

import io.rheem.basic.data.Record;
import io.rheem.basic.operators.FilterOperator;
import io.rheem.core.api.Configuration;
import io.rheem.core.function.PredicateDescriptor;
import io.rheem.core.optimizer.costs.LoadProfileEstimator;
import io.rheem.core.optimizer.costs.LoadProfileEstimators;
import io.rheem.jdbc.compiler.FunctionCompiler;

import java.sql.Connection;
import java.util.Optional;


/**
 * Template for JDBC-based {@link FilterOperator}.
 */
public abstract class JdbcFilterOperator extends FilterOperator<Record> implements JdbcExecutionOperator {

    public JdbcFilterOperator(PredicateDescriptor<Record> predicateDescriptor) {
        super(predicateDescriptor);
    }

    public JdbcFilterOperator(FilterOperator<Record> that) {
        super(that);
    }

    @Override
    public String createSqlClause(Connection connection, FunctionCompiler compiler) {
        return compiler.compile(this.predicateDescriptor);
    }

    @Override
    public String getLoadProfileEstimatorConfigurationKey() {
        return String.format("rheem.%s.filter.load", this.getPlatform().getPlatformId());
    }

    @Override
    public Optional<LoadProfileEstimator> createLoadProfileEstimator(Configuration configuration) {
        final Optional<LoadProfileEstimator> optEstimator =
                JdbcExecutionOperator.super.createLoadProfileEstimator(configuration);
        LoadProfileEstimators.nestUdfEstimator(optEstimator, this.predicateDescriptor, configuration);
        return optEstimator;
    }
}
