package com.xxl.job.core.constant;

/**
 * XxlJobBoost 任务调度类型。
 */
public enum XxlJobScheduleType {

    /**
     * 不启用自动调度。
     */
    NONE,

    /**
     * Cron 表达式调度。
     */
    CRON,

    /**
     * 固定频率调度，调度配置为秒数。
     */
    FIX_RATE
}
