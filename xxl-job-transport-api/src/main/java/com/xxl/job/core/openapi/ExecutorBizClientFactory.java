package com.xxl.job.core.openapi;

import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;

public class ExecutorBizClientFactory {

    public static ExecutorBiz getExecutorBiz(String address, String accessToken, int timeout) {
        return ExecutorBizClientTransportFactory.getExecutorBiz(address, accessToken, timeout);
    }

    private ExecutorBizClientFactory() {
    }
}
