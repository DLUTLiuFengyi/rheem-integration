package io.rheem.java.plugin;

import io.rheem.core.api.Configuration;
import io.rheem.core.mapping.Mapping;
import io.rheem.core.optimizer.channels.ChannelConversion;
import io.rheem.core.plan.rheemplan.Operator;
import io.rheem.core.platform.Platform;
import io.rheem.core.plugin.Plugin;
import io.rheem.java.mapping.Mappings;
import io.rheem.java.platform.JavaPlatform;

import java.util.Collection;
import java.util.Collections;

/**
 * This {@link Plugin} enables to use the basic Rheem {@link Operator}s on the {@link JavaPlatform}.
 */
public class JavaGraphPlugin implements Plugin {

    @Override
    public Collection<Mapping> getMappings() {
        return Mappings.GRAPH_MAPPINGS;
    }

    @Override
    public Collection<ChannelConversion> getChannelConversions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Platform> getRequiredPlatforms() {
        return Collections.singleton(JavaPlatform.getInstance());
    }

    @Override
    public void setProperties(Configuration configuration) {
        // Nothing to do, because we already configured the properties in #configureDefaults(...).
    }

}
