package com.xxl.job.core.handler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJobBoost {

    String desc();

    String author();

    String jobTag() default "";

    String alarmEmail() default "";

    String alarmChannelIds() default "";

    String alarmEventTypes() default "";

    String scheduleType() default "CRON";

    String scheduleConf() default "";

    String executorParam() default "";

    String routeStrategy() default "FIRST";

    String misfireStrategy() default "DO_NOTHING";

    String blockStrategy() default "SERIAL_EXECUTION";

    int timeout() default 0;

    int retryCount() default 0;

    boolean autoStart() default false;
}
