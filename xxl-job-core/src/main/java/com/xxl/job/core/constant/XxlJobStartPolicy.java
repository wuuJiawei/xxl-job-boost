package com.xxl.job.core.constant;

/**
 * XxlJobBoost 任务启动策略。
 */
public enum XxlJobStartPolicy {

    /**
     * 新任务创建后保持停止，已有任务保留调度中心中的运行状态。
     */
    MANUAL,

    /**
     * 新任务创建后自动启动，已有任务保留调度中心中的运行状态。
     */
    ON_CREATE,

    /**
     * 新任务创建后自动启动，并在后续同步时确保已有任务处于运行状态。
     */
    ENSURE_RUNNING;

    public static XxlJobStartPolicy match(String value) {
        if (value != null) {
            for (XxlJobStartPolicy policy : values()) {
                if (policy.name().equalsIgnoreCase(value.trim())) {
                    return policy;
                }
            }
        }
        return MANUAL;
    }
}
