package com.xxl.job.core.constant;

/**
 * XxlJobBoost 执行器路由策略。
 */
public enum XxlJobRouteStrategy {

    /**
     * 第一个可用地址。
     */
    FIRST,

    /**
     * 最后一个可用地址。
     */
    LAST,

    /**
     * 轮询。
     */
    ROUND,

    /**
     * 随机。
     */
    RANDOM,

    /**
     * 一致性哈希。
     */
    CONSISTENT_HASH,

    /**
     * 最不经常使用。
     */
    LEAST_FREQUENTLY_USED,

    /**
     * 最近最久未使用。
     */
    LEAST_RECENTLY_USED,

    /**
     * 故障转移。
     */
    FAILOVER,

    /**
     * 忙碌转移。
     */
    BUSYOVER,

    /**
     * 分片广播。
     */
    SHARDING_BROADCAST
}
