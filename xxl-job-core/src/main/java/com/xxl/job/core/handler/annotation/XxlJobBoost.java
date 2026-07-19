package com.xxl.job.core.handler.annotation;

import com.xxl.job.core.constant.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * XXL-JOB Boost 任务声明注解。
 *
 * <p>该注解在注册 JobHandler 的同时声明任务同步元数据。执行器启动扫描后，
 * {@code JobSyncHelper} 会把枚举属性序列化为 XXL-JOB admin 兼容的名称字符串。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJobBoost {

    /**
     * JobHandler 名称。与 {@link XxlJob#value()} 含义一致。
     *
     * <p>当方法同时声明 {@link XxlJob} 时可留空，优先复用 {@link XxlJob#value()}。</p>
     */
    String value() default "";

    /**
     * 任务初始化方法名。与 {@link XxlJob#init()} 含义一致。
     */
    String init() default "";

    /**
     * 任务销毁方法名。与 {@link XxlJob#destroy()} 含义一致。
     */
    String destroy() default "";

    /**
     * 任务描述，对应 admin 侧的任务描述。
     */
    String desc();

    /**
     * 负责人，对应 admin 侧的负责人。
     */
    String author();

    /**
     * 任务标签，多个标签使用英文逗号分隔。
     */
    String jobTag() default "";

    /**
     * 告警邮箱，多个邮箱使用英文逗号分隔。
     */
    String alarmEmail() default "";

    /**
     * 告警渠道 ID，多个渠道使用英文逗号分隔。
     */
    String alarmChannelIds() default "";

    /**
     * 触发告警的事件类型。留空表示使用 admin 侧默认失败事件范围。
     */
    XxlJobAlarmEventType[] alarmEventTypes() default {};

    /**
     * 调度类型，默认使用 CRON。
     */
    XxlJobScheduleType scheduleType() default XxlJobScheduleType.CRON;

    /**
     * 调度配置。CRON 类型填写 Cron 表达式，FIX_RATE 类型填写固定频率秒数，NONE 类型可留空。
     */
    String scheduleConf() default "";

    /**
     * 默认任务参数。
     */
    String executorParam() default "";

    /**
     * 执行器路由策略，默认选择第一个可用执行器地址。
     */
    XxlJobRouteStrategy routeStrategy() default XxlJobRouteStrategy.FIRST;

    /**
     * 调度过期策略，默认忽略过期触发。
     */
    XxlJobMisfireStrategy misfireStrategy() default XxlJobMisfireStrategy.DO_NOTHING;

    /**
     * 阻塞处理策略，默认串行执行。
     */
    ExecutorBlockStrategyEnum blockStrategy() default ExecutorBlockStrategyEnum.SERIAL_EXECUTION;

    /**
     * 任务超时时间，单位秒。0 表示不限制。
     */
    int timeout() default 0;

    /**
     * 失败重试次数。0 表示不重试。
     */
    int retryCount() default 0;

    /**
     * 任务启动策略。默认由调度中心人工管理运行状态。
     */
    XxlJobStartPolicy startPolicy() default XxlJobStartPolicy.MANUAL;
}
