package com.xxl.job.admin.core.trigger;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.client.ExecutorBizEndpoint;
import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultExecutorBizProvider implements ExecutorBizProvider {

    private final ExecutorBizEndpointResolver endpointResolver;
    private final String accessToken;
    private final int timeout;

    public DefaultExecutorBizProvider(ExecutorBizEndpointResolver endpointResolver,
                                      @Value("${xxl.job.accessToken}") String accessToken,
                                      @Value("${xxl.job.timeout}") int timeout) {
        this.endpointResolver = endpointResolver;
        this.accessToken = accessToken;
        this.timeout = timeout;
    }

    @Override
    public ExecutorBiz getExecutorBiz(String address) {
        String rawEndpoint = endpointResolver.normalize(address);
        return ExecutorBizClientTransportFactory.getExecutorBiz(rawEndpoint, accessToken, timeout);
    }
}
