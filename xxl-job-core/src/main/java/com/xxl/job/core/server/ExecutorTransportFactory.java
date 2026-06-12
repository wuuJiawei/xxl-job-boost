package com.xxl.job.core.server;

public class ExecutorTransportFactory {

    public static ExecutorTransport create(String transportType) {
        ExecutorTransportType type = ExecutorTransportType.match(transportType);
        switch (type) {
            case SPRING_HTTP:
                return new SpringHttpExecutorTransport();
            case NETTY_EMBED:
            default:
                return new NettyEmbedExecutorTransport();
        }
    }
}
