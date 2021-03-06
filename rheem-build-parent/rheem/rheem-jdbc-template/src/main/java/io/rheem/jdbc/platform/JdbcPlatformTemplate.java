package io.rheem.jdbc.platform;

import io.rheem.core.api.Configuration;
import io.rheem.core.optimizer.costs.LoadProfileToTimeConverter;
import io.rheem.core.optimizer.costs.LoadToTimeConverter;
import io.rheem.core.optimizer.costs.TimeToCostConverter;
import io.rheem.core.platform.ChannelDescriptor;
import io.rheem.core.platform.Executor;
import io.rheem.core.platform.Platform;
import io.rheem.core.util.ReflectionUtils;
import io.rheem.jdbc.channels.SqlQueryChannel;
import io.rheem.jdbc.execution.DatabaseDescriptor;
import io.rheem.jdbc.execution.JdbcExecutor;

import java.sql.Connection;

/**
 * {@link Platform} implementation for a JDBC-accessible database.
 */
public abstract class JdbcPlatformTemplate extends Platform {

    public final String cpuMhzProperty = String.format("rheem.%s.cpu.mhz", this.getPlatformId());

    public final String coresProperty = String.format("rheem.%s.cores", this.getPlatformId());

    public final String jdbcUrlProperty = String.format("rheem.%s.jdbc.url", this.getPlatformId());

    public final String jdbcUserProperty = String.format("rheem.%s.jdbc.user", this.getPlatformId());

    public final String jdbcPasswordProperty = String.format("rheem.%s.jdbc.password", this.getPlatformId());

    private String getDefaultConfigurationFile() {
        return String.format("rheem-%s-defaults.properties", this.getPlatformId());
    }

    /**
     * {@link ChannelDescriptor} for {@link SqlQueryChannel}s with this instance.
     */
    private final SqlQueryChannel.Descriptor sqlQueryChannelDescriptor = new SqlQueryChannel.Descriptor(this);

    public Connection getConnection() {
        return connection;
    }

    private Connection connection = null;

    protected JdbcPlatformTemplate(String platformName, String configName) {
        super(platformName, configName);
    }

    @Override
    public void configureDefaults(Configuration configuration) {
        configuration.load(ReflectionUtils.loadResource(this.getDefaultConfigurationFile()));
    }

    @Override
    public Executor.Factory getExecutorFactory() {
        return job -> new JdbcExecutor(this, job);
    }

    @Override
    public LoadProfileToTimeConverter createLoadProfileToTimeConverter(Configuration configuration) {
        int cpuMhz = (int) configuration.getLongProperty(this.cpuMhzProperty);
        int numCores = (int) configuration.getLongProperty(this.coresProperty);
        return LoadProfileToTimeConverter.createDefault(
                LoadToTimeConverter.createLinearCoverter(1 / (numCores * cpuMhz * 1000.)),
                LoadToTimeConverter.createLinearCoverter(0),
                LoadToTimeConverter.createLinearCoverter(0),
                (cpuEstimate, diskEstimate, networkEstimate) -> cpuEstimate.plus(diskEstimate).plus(networkEstimate)
        );
    }

    @Override
    public TimeToCostConverter createTimeToCostConverter(Configuration configuration) {
        return new TimeToCostConverter(
                configuration.getDoubleProperty(String.format("rheem.%s.costs.fix", this.getPlatformId())),
                configuration.getDoubleProperty(String.format("rheem.%s.costs.per-ms", this.getPlatformId()))
        );
    }

    /**
     * Provide a unique identifier for this kind of platform. Should consist of alphanumerical characters only.
     *
     * @return the platform ID
     */
    public String getPlatformId() {
        return this.getConfigurationName();
    }

    /**
     * Provide the name of the JDBC driver {@link Class} for this instance.
     *
     * @return the driver {@link Class} name
     */
    protected abstract String getJdbcDriverClassName();

    /**
     * Retrieve a {@link SqlQueryChannel.Descriptor} for this instance.
     *
     * @return the {@link SqlQueryChannel.Descriptor}
     */
    public SqlQueryChannel.Descriptor getSqlQueryChannelDescriptor() {
        return this.sqlQueryChannelDescriptor;
    }

    /**
     * Creates a new {@link DatabaseDescriptor} for this instance and the given {@link Configuration}.
     *
     * @param configuration provides configuration information for the result
     * @return the {@link DatabaseDescriptor}
     */
    public DatabaseDescriptor createDatabaseDescriptor(Configuration configuration) {
        return new DatabaseDescriptor(
                configuration.getStringProperty(this.jdbcUrlProperty),
                configuration.getStringProperty(this.jdbcUserProperty, null),
                configuration.getStringProperty(this.jdbcPasswordProperty, null),
                this.getJdbcDriverClassName()
        );
    }
}
