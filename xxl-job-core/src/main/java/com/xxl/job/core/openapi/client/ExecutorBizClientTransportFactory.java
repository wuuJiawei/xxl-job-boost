package com.xxl.job.core.openapi.client;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.tool.core.StringTool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExecutorBizClientTransportFactory {

    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_REPOSITORY = new ConcurrentHashMap<String, ExecutorBiz>();

    public static ExecutorBiz getExecutorBiz(String address, String accessToken, int timeout) {
        ExecutorBizEndpoint endpoint = ExecutorBizEndpointParser.parse(address);
        if (StringTool.isBlank(endpoint.getAddress())) {
            return null;
        }

        ExecutorBizClientTransport transport = match(endpoint);
        String normalizedAddress = transport.normalizeAddress(endpoint.getAddress());
        String cacheKey = buildCacheKey(transport.type(), normalizedAddress, accessToken, timeout);
        ExecutorBiz executorBiz = EXECUTOR_BIZ_REPOSITORY.get(cacheKey);
        if (executorBiz != null) {
            return executorBiz;
        }

        executorBiz = transport.create(normalizedAddress, accessToken, timeout);
        EXECUTOR_BIZ_REPOSITORY.put(cacheKey, executorBiz);
        return executorBiz;
    }

    public static ExecutorBizClientTransport match(String address) {
        return match(ExecutorBizEndpointParser.parse(address));
    }

    public static ExecutorBizClientTransport match(ExecutorBizEndpoint endpoint) {
        if (endpoint == null || StringTool.isBlank(endpoint.getAddress())) {
            throw new IllegalArgumentException("Unsupported executor address: " + endpoint);
        }
        List<ExecutorBizClientTransport> transports = ExecutorBizClientTransportRegistry.list();
        for (ExecutorBizClientTransport transport : transports) {
            if (endpoint.hasExplicitTransport()) {
                if (transport.type().equalsIgnoreCase(endpoint.getTransport())) {
                    return transport;
                }
                continue;
            }
            if (transport.supports(endpoint.getAddress())) {
                return transport;
            }
        }
        throw new IllegalArgumentException("Unsupported executor address: " + endpoint.getAddress());
    }

    private static String buildCacheKey(String type, String address, String accessToken, int timeout) {
        return type + "|" + address + "|" + timeout + "|" + (accessToken == null ? "" : accessToken);
    }

    private ExecutorBizClientTransportFactory() {
    }
}
