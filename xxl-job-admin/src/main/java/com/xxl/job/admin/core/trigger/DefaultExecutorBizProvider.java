package com.xxl.job.admin.core.trigger;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultExecutorBizProvider implements ExecutorBizProvider {

    private final String accessToken;
    private final int timeout;

    public DefaultExecutorBizProvider(@Value("${xxl.job.accessToken}") String accessToken,
                                      @Value("${xxl.job.timeout}") int timeout) {
        this.accessToken = accessToken;
        this.timeout = timeout;
    }

    @Override
    public ExecutorBiz getExecutorBiz(String address) {
        return ExecutorBizClientTransportFactory.getExecutorBiz(address, accessToken, timeout);
    }
}
