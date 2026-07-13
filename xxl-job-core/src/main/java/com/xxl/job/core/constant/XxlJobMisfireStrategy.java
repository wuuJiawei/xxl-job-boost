package com.xxl.job.core.constant;

/**
 * XxlJobBoost 调度过期处理策略。
 */
public enum XxlJobMisfireStrategy {

    /**
     * 忽略过期触发。
     */
    DO_NOTHING,

    /**
     * 立即补偿触发一次。
     */
    FIRE_ONCE_NOW
}
