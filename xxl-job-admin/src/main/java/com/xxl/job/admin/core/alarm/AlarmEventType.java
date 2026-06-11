package com.xxl.job.admin.core.alarm;

import com.xxl.job.core.context.XxlJobContext;

public enum AlarmEventType {
    EXECUTOR_FAIL("执行失败"),
    EXECUTOR_TIMEOUT("执行超时"),
    TRIGGER_FAIL("触发失败");

    private final String title;

    AlarmEventType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static AlarmEventType match(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (AlarmEventType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }

    public static AlarmEventType of(int triggerCode, int handleCode) {
        if (triggerCode != 0 && triggerCode != XxlJobContext.HANDLE_CODE_SUCCESS) {
            return TRIGGER_FAIL;
        }
        if (handleCode == XxlJobContext.HANDLE_CODE_TIMEOUT) {
            return EXECUTOR_TIMEOUT;
        }
        if (handleCode != 0 && handleCode != XxlJobContext.HANDLE_CODE_SUCCESS) {
            return EXECUTOR_FAIL;
        }
        return null;
    }
}
