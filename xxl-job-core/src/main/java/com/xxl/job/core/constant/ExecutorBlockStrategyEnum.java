package com.xxl.job.core.constant;

/**
 * 执行器阻塞处理策略。
 *
 * <p>当同一个任务在执行器侧已有运行中的实例时，调度中心会把该策略随触发请求下发给执行器，
 * 执行器据此决定新触发请求和旧运行实例的处理方式。</p>
 */
public enum ExecutorBlockStrategyEnum {

    /**
     * 串行执行。
     *
     * <p>已有实例运行时，新触发请求进入等待队列，等前一个实例结束后再执行。</p>
     */
    SERIAL_EXECUTION("Serial execution"),

    /**
     * 并行执行。
     *
     * <p>当前版本暂未启用，保留注释用于说明历史策略。</p>
     */
    /*CONCURRENT_EXECUTION("并行"),*/

    /**
     * 丢弃后续调度。
     *
     * <p>已有实例运行时，新的触发请求直接失败返回，不进入等待队列。</p>
     */
    DISCARD_LATER("Discard Later"),

    /**
     * 覆盖之前调度。
     *
     * <p>已有实例运行时，终止旧实例并执行新的触发请求。</p>
     */
    COVER_EARLY("Cover Early");

    private String title;
    private ExecutorBlockStrategyEnum (String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    /**
     * 根据枚举名称匹配阻塞策略。
     *
     * @param name 枚举名称
     * @param defaultItem 未匹配时返回的默认值
     * @return 匹配到的阻塞策略，或默认值
     */
    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
