package com.xxl.job.core.server;

import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;

public interface ExecutorTransport {

    String type();

    void start(String address, int port, String appname, String accessToken) throws Exception;

    void stop() throws Exception;

    default String registryValue(String address) {
        return ExecutorBizClientTransportFactory.normalizeEndpoint(address);
    }
}
