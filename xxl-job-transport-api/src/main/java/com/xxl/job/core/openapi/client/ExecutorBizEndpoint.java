package com.xxl.job.core.openapi.client;

import com.xxl.tool.core.StringTool;

public class ExecutorBizEndpoint {
    private final String transport;
    private final String address;

    public ExecutorBizEndpoint(String transport, String address) {
        this.transport = transport == null ? null : transport.trim();
        this.address = address == null ? null : address.trim();
    }

    public String getTransport() {
        return transport;
    }

    public String getAddress() {
        return address;
    }

    public boolean hasExplicitTransport() {
        return StringTool.isNotBlank(transport);
    }
}
