package com.xxl.job.core.server;

/**
 * @deprecated use {@link NettyEmbedExecutorTransport} via {@link ExecutorTransportFactory}
 */
@Deprecated
public class EmbedServer {
    private final ExecutorTransport delegate = new NettyEmbedExecutorTransport();

    public void start(final String address, final int port, final String appname, final String accessToken) throws Exception {
        delegate.start(address, port, appname, accessToken);
    }

    public void stop() throws Exception {
        delegate.stop();
    }
}
