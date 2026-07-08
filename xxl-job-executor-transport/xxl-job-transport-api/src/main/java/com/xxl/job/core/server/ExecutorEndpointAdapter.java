package com.xxl.job.core.server;

import java.util.EnumSet;
import java.util.Set;

public interface ExecutorEndpointAdapter {

    String framework();

    Set<ExecutorEndpoint> supportedEndpoints();

    default void assertSupportedAllRequiredEndpoints() {
        EnumSet<ExecutorEndpoint> missing = EnumSet.allOf(ExecutorEndpoint.class);
        missing.removeAll(supportedEndpoints());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(framework() + " missing executor endpoints: " + missing);
        }
    }
}
