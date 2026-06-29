package com.xxl.job.core.openapi.client;

import com.xxl.tool.core.StringTool;

public class ExecutorBizEndpointParser {

    public static ExecutorBizEndpoint parse(String value) {
        if (StringTool.isBlank(value)) {
            return new ExecutorBizEndpoint(null, null);
        }

        int delimiterIndex = value.indexOf("::");
        if (delimiterIndex < 0) {
            return new ExecutorBizEndpoint(null, value.trim());
        }

        String transport = value.substring(0, delimiterIndex).trim();
        String address = value.substring(delimiterIndex + 2).trim();
        if (StringTool.isBlank(transport) || StringTool.isBlank(address)) {
            return new ExecutorBizEndpoint(null, value.trim());
        }

        return new ExecutorBizEndpoint(transport, address);
    }

    private ExecutorBizEndpointParser() {
    }
}
