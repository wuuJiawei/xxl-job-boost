package com.xxl.job.core.executor;

import com.xxl.job.core.constant.ExecutorBlockStrategyEnum;
import com.xxl.job.core.constant.XxlJobAlarmEventType;
import com.xxl.job.core.constant.XxlJobMisfireStrategy;
import com.xxl.job.core.constant.XxlJobRouteStrategy;
import com.xxl.job.core.constant.XxlJobScheduleType;
import com.xxl.job.core.handler.annotation.XxlJobBoost;
import com.xxl.job.core.openapi.model.JobSyncItem;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class JobSyncHelperTest {

    @Test
    public void toItemShouldSerializeBoostEnumsToAdminNames() throws Exception {
        Method method = TestJobBean.class.getDeclaredMethod("demoJobHandler");
        XxlJobBoost xxlJobBoost = method.getAnnotation(XxlJobBoost.class);

        JobSyncItem item = JobSyncHelper.toItem("demoJobHandler", xxlJobBoost, method);

        Assert.assertNotNull(item);
        Assert.assertEquals("EXECUTOR_FAIL,TRIGGER_FAIL", item.getAlarmEventTypes());
        Assert.assertEquals("CRON", item.getScheduleType());
        Assert.assertEquals("FIRST", item.getExecutorRouteStrategy());
        Assert.assertEquals("DO_NOTHING", item.getMisfireStrategy());
        Assert.assertEquals("SERIAL_EXECUTION", item.getExecutorBlockStrategy());
    }

    private static class TestJobBean {

        @XxlJobBoost(
                desc = "demo job",
                author = "XXL",
                alarmEventTypes = {XxlJobAlarmEventType.EXECUTOR_FAIL, XxlJobAlarmEventType.TRIGGER_FAIL},
                scheduleType = XxlJobScheduleType.CRON,
                routeStrategy = XxlJobRouteStrategy.FIRST,
                misfireStrategy = XxlJobMisfireStrategy.DO_NOTHING,
                blockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION
        )
        public void demoJobHandler() {
        }
    }
}
