package com.xxl.job.core.openapi.client;

import com.xxl.tool.core.StringTool;

public class ExecutorBizEndpointParser {

    public static ExecutorBizEndpoint parse(String rawEndpoint) {
        if (StringTool.isBlank(rawEndpoint)) {
            return new ExecutorBizEndpoint(null, null);
        }

        String value = rawEndpoint.trim();
        int delimiterIndex = value.indexOf("::");
        if (delimiterIndex <= 0) {
            return new ExecutorBizEndpoint(null, value);
        }

        String transport = value.substring(0, delimiterIndex).trim();
        String address = value.substring(delimiterIndex + 2).trim();
        if (StringTool.isBlank(transport) || StringTool.isBlank(address)) {
            throw new IllegalArgumentException("Invalid executor endpoint: " + rawEndpoint);
        }

        return new ExecutorBizEndpoint(transport, address);
    }

    private ExecutorBizEndpointParser() {
    }
}
