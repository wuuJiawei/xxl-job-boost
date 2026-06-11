package com.xxl.job.admin.core.alarm;

public enum AlarmChannelType {
    EMAIL,
    WEBHOOK,
    FEISHU,
    WECOM,
    DINGTALK;

    public static AlarmChannelType match(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (AlarmChannelType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
