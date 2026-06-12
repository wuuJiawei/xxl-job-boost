package com.xxl.job.core.openapi.client;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.tool.core.StringTool;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExecutorBizClientTransportFactory {

    private static final List<ExecutorBizClientTransport> TRANSPORTS = Arrays.asList(
            new HttpExecutorBizClientTransport()
    );
    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_REPOSITORY = new ConcurrentHashMap<String, ExecutorBiz>();

    public static ExecutorBiz getExecutorBiz(String address, String accessToken, int timeout) {
        if (StringTool.isBlank(address)) {
            return null;
        }

        ExecutorBizClientTransport transport = match(address);
        String normalizedAddress = transport.normalizeAddress(address);
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
        for (ExecutorBizClientTransport transport : TRANSPORTS) {
            if (transport.supports(address)) {
                return transport;
            }
        }
        throw new IllegalArgumentException("Unsupported executor address: " + address);
    }

    private static String buildCacheKey(ExecutorBizClientTransportType type, String address, String accessToken, int timeout) {
        return type.name() + "|" + address + "|" + timeout + "|" + (accessToken == null ? "" : accessToken);
    }

    private ExecutorBizClientTransportFactory() {
    }
}
