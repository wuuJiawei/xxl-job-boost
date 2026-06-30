package com.xxl.job.core.executor.impl;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.annotation.XxlJobBoost;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class XxlJobSpringExecutorTest {

    @Before
    @After
    public void clearJobHandlers() throws Exception {
        Field repository = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
        repository.setAccessible(true);
        ((Map<?, ?>) repository.get(null)).clear();
    }

    @Test
    public void scanJobHandlerMethodShouldSupportXxlJobAndXxlJobBoostCombinations() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestJobBean.class);
        context.refresh();

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        Method scanMethod = XxlJobSpringExecutor.class.getDeclaredMethod("scanJobHandlerMethod", org.springframework.context.ApplicationContext.class);
        scanMethod.setAccessible(true);
        scanMethod.invoke(executor, context);

        Assert.assertNotNull(XxlJobExecutor.loadJobHandler("legacyJobHandler"));
        Assert.assertNotNull(XxlJobExecutor.loadJobHandler("dualJobHandler"));
        Assert.assertNotNull(XxlJobExecutor.loadJobHandler("boostOnlyJobHandler"));

        context.close();
    }

    @Test
    public void scanJobHandlerMethodShouldFailWhenBoostOnlyHandlerNameIsBlank() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(BlankBoostJobBean.class);
        context.refresh();

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        Method scanMethod = XxlJobSpringExecutor.class.getDeclaredMethod("scanJobHandlerMethod", org.springframework.context.ApplicationContext.class);
        scanMethod.setAccessible(true);

        try {
            scanMethod.invoke(executor, context);
            Assert.fail("blank @XxlJobBoost value should fail");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Assert.assertTrue(e.getCause().getMessage().contains("method-jobhandler name invalid"));
        } finally {
            context.close();
        }
    }

    @Component
    public static class TestJobBean {
        @XxlJob("legacyJobHandler")
        public void legacyJobHandler() {
        }

        @XxlJob("dualJobHandler")
        @XxlJobBoost(desc = "dual job", author = "XXL")
        public void dualJobHandler() {
        }

        @XxlJobBoost(value = "boostOnlyJobHandler", desc = "boost only job", author = "XXL")
        public void boostOnlyJobHandler() {
        }
    }

    @Component
    public static class BlankBoostJobBean {
        @XxlJobBoost(desc = "blank boost job", author = "XXL")
        public void blankBoostJobHandler() {
        }
    }
}
