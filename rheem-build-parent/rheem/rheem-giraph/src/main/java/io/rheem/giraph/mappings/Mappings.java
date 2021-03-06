package io.rheem.giraph.mappings;

import io.rheem.core.mapping.Mapping;

import java.util.Collection;
import java.util.Collections;

/**
 * Register for {@link Mapping}s for this platform.
 */
public class Mappings {

    public static final Collection<Mapping> ALL = Collections.singletonList(
            new PageRankMapping()
    );
}
