package com.xxl.job.core.server;

import com.xxl.job.core.openapi.ExecutorBiz;

/**
 * @deprecated use {@link ExecutorEndpointDispatcher}
 */
@Deprecated
public class ExecutorTransportDispatcher extends ExecutorEndpointDispatcher {

    public ExecutorTransportDispatcher(ExecutorBiz executorBiz, String accessToken) {
        super(executorBiz, accessToken);
    }
}
