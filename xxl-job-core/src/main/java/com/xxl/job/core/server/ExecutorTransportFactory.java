package com.xxl.job.core.server;

public class ExecutorTransportFactory {

    public static ExecutorTransport create(String transportType) {
        return ExecutorTransportRegistry.get(transportType);
    }
}
