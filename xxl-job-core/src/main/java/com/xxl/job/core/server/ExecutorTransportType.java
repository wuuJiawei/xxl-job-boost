package com.xxl.job.core.server;

import com.xxl.tool.core.StringTool;

public enum ExecutorTransportType {
    NETTY_EMBED,
    SPRING_HTTP;

    public static ExecutorTransportType match(String value) {
        if (StringTool.isBlank(value)) {
            return NETTY_EMBED;
        }
        for (ExecutorTransportType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return NETTY_EMBED;
    }
}
