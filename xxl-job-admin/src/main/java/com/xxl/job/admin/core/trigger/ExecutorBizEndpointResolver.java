package com.xxl.job.admin.core.trigger;

import com.xxl.job.core.openapi.client.ExecutorBizEndpoint;
import com.xxl.job.core.openapi.client.ExecutorBizEndpointParser;
import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;
import org.springframework.stereotype.Component;

@Component
public class ExecutorBizEndpointResolver {

    public ExecutorBizEndpoint resolve(String endpoint) {
        ExecutorBizEndpoint parsed = ExecutorBizEndpointParser.parse(endpoint);
        ExecutorBizClientTransportFactory.match(parsed);
        return parsed;
    }

    public boolean supports(String endpoint) {
        try {
            resolve(endpoint);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
