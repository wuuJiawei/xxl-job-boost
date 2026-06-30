package com.xxl.job.core.executor.impl;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.JobSyncHelper;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.annotation.XxlJobBoost;
import com.xxl.job.core.openapi.model.JobSyncItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * xxl-job executor (for spring)
 *
 * @author xuxueli 2018-11-01 09:24:52
 */
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobSpringExecutor.class);

    // ---------------------- field ----------------------

    /**
     * excluded package, like "org.springframework"、"org.aaa,org.bbb"
     */
    private String excludedPackage = "org.springframework.,spring.";

    public void setExcludedPackage(String excludedPackage) {
        this.excludedPackage = excludedPackage;
    }


    // ---------------------- start / stop ----------------------

    /**
     * start
      */
    @Override
    public void afterSingletonsInstantiated() {

        // scan JobHandler method
        scanJobHandlerMethod(applicationContext);

        // refresh GlueFactory
        GlueFactory.refreshInstance(1);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * stop
      */
    @Override
    public void destroy() {
        super.destroy();
    }


    /**
     * init job handler from method
     *
     * @param applicationContext applicationContext
     */
    private void scanJobHandlerMethod(ApplicationContext applicationContext) {
        // valid
        if (applicationContext == null) {
            return;
        }

        List<JobSyncItem> jobSyncItems = JobSyncHelper.newItems();

        // 1、build excluded-package list
        List<String> excludedPackageList = new ArrayList<>();
        if (excludedPackage != null) {
            for (String excludedPackage : excludedPackage.split(",")) {
                if (!excludedPackage.trim().isEmpty()){
                    excludedPackageList.add(excludedPackage.trim());
                }
            }
        }

        // 2、scan bean form jobhandler
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, false);  // allowEagerInit=false, avoid early initialization
        for (String beanName : beanNames) {

            /**
             * 2.1、skip by BeanDefinition:
             *      - skip excluded-package bean
             *      - skip lazy-init bean
              */
            if (applicationContext instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                // get BeanDefinition
                if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
                    continue;
                }
                BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);

                // skip excluded-package bean
                String beanClassName = beanDefinition.getBeanClassName();
                if (isExcluded(excludedPackageList, beanClassName)) {
                    logger.debug(">>>>>>>>>>> xxl-job bean-definition scan, skip excluded-package beanName:{}, beanClassName:{}", beanName, beanClassName);
                    continue;
                }

                // skip lazy-init bean
                if (beanDefinition.isLazyInit()) {
                    logger.debug(">>>>>>>>>>> xxl-job bean-definition scan, skip lazy-init beanName:{}", beanName);
                    continue;
                }
            }

            /**
             * 2.2、skip by BeanDefinition Class
             *      - skip beanClass is null
             *      - skip method annotation(@XxlJob/@XxlJobBoost) is null
             */
            Class<?> beanClass = applicationContext.getType(beanName, false);
            if (beanClass == null) {
                logger.debug(">>>>>>>>>>> xxl-job bean-definition scan, skip beanClass-null beanName:{}", beanName);
                continue;
            }
            // filter method
            Map<Method, XxlJobMethodMetadata> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(beanClass,
                        new MethodIntrospector.MetadataLookup<XxlJobMethodMetadata>() {
                            @Override
                            public XxlJobMethodMetadata inspect(Method method) {
                                XxlJob xxlJob = AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
                                XxlJobBoost xxlJobBoost = AnnotatedElementUtils.findMergedAnnotation(method, XxlJobBoost.class);
                                if (xxlJob == null && xxlJobBoost == null) {
                                    return null;
                                }
                                return new XxlJobMethodMetadata(xxlJob, xxlJobBoost);
                            }
                        });
            } catch (Throwable ex) {
                logger.error(">>>>>>>>>>> xxl-job method-jobhandler resolve error for bean[" + beanName + "].", ex);
            }
            if (annotatedMethods==null || annotatedMethods.isEmpty()) {
                continue;
            }

            // 2.3、scan + registry Jobhandler
            Object jobBean = applicationContext.getBean(beanName);
            for (Map.Entry<Method, XxlJobMethodMetadata> jobMethodEntry : annotatedMethods.entrySet()) {
                Method jobMethod = jobMethodEntry.getKey();
                XxlJobMethodMetadata metadata = jobMethodEntry.getValue();
                String handlerName = metadata.handlerName();
                // regist
                registryJobHandler(handlerName, metadata.init(), metadata.destroy(), jobBean, jobMethod);

                JobSyncItem item = JobSyncHelper.toItem(handlerName, metadata.xxlJobBoost, jobMethod);
                if (item != null) {
                    jobSyncItems.add(item);
                }
            }

        }

        JobSyncHelper.sync(this, jobSyncItems);
    }

    /**
     * check bean if excluded
     *
     * @param excludedPackageList   excludedPackageList
     * @param beanClassName         beanClassName
     * @return  true if excluded
     */
    private boolean isExcluded(List<String> excludedPackageList, String beanClassName) {
        // excludedPackageList is empty, no excluded
        if (excludedPackageList == null || excludedPackageList.isEmpty()) {
            return false;
        }

        // beanClassName is null, no excluded
        if (beanClassName == null) {
            return false;
        }

        // excludedPackageList match, excluded (not scan)
        for (String excludedPackage : excludedPackageList) {
            if (beanClassName.startsWith(excludedPackage)) {
                return true;
            }
        }
        return false;
    }

    private static class XxlJobMethodMetadata {
        private final XxlJob xxlJob;
        private final XxlJobBoost xxlJobBoost;

        private XxlJobMethodMetadata(XxlJob xxlJob, XxlJobBoost xxlJobBoost) {
            this.xxlJob = xxlJob;
            this.xxlJobBoost = xxlJobBoost;
        }

        private String handlerName() {
            if (xxlJob != null) {
                return xxlJob.value();
            }
            return xxlJobBoost != null ? xxlJobBoost.value() : "";
        }

        private String init() {
            if (xxlJob != null) {
                return xxlJob.init();
            }
            return xxlJobBoost != null ? xxlJobBoost.init() : "";
        }

        private String destroy() {
            if (xxlJob != null) {
                return xxlJob.destroy();
            }
            return xxlJobBoost != null ? xxlJobBoost.destroy() : "";
        }
    }


    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobSpringExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
