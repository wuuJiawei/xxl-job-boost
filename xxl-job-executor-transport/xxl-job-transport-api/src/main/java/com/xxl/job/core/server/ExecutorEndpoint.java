package com.xxl.job.core.server;

public enum ExecutorEndpoint {

    BEAT(ExecutorEndpointPaths.BEAT, false),
    IDLE_BEAT(ExecutorEndpointPaths.IDLE_BEAT, true),
    RUN(ExecutorEndpointPaths.RUN, true),
    KILL(ExecutorEndpointPaths.KILL, true),
    LOG(ExecutorEndpointPaths.LOG, true);

    private final String path;
    private final boolean requestBodyRequired;

    ExecutorEndpoint(String path, boolean requestBodyRequired) {
        this.path = path;
        this.requestBodyRequired = requestBodyRequired;
    }

    public String path() {
        return path;
    }

    public boolean requestBodyRequired() {
        return requestBodyRequired;
    }

    public static ExecutorEndpoint fromPath(String path) {
        if (path == null) {
            return null;
        }
        for (ExecutorEndpoint endpoint : values()) {
            if (endpoint.path.equals(path)) {
                return endpoint;
            }
        }
        return null;
    }
}
