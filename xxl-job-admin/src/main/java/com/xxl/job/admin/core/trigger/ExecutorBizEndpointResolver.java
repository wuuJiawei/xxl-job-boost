package com.xxl.job.admin.core.trigger;

import com.xxl.job.core.openapi.client.ExecutorBizEndpoint;
import com.xxl.job.core.openapi.client.ExecutorBizEndpointParser;
import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;
import org.springframework.stereotype.Component;

@Component
public class ExecutorBizEndpointResolver {

    public ExecutorBizEndpoint resolve(String endpoint) {
        return ExecutorBizEndpointParser.parse(normalize(endpoint));
    }

    public String normalize(String endpoint) {
        return ExecutorBizClientTransportFactory.normalizeEndpoint(endpoint);
    }

    public boolean supports(String endpoint) {
        try {
            normalize(endpoint);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
