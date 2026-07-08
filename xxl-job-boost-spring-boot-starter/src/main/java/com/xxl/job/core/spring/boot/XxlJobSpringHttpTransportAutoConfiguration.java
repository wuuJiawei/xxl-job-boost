package com.xxl.job.core.spring.boot;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.server.ExecutorTransportDispatcher;
import com.xxl.job.core.server.ExecutorTransportFactory;
import com.xxl.job.core.server.SpringHttpExecutorTransportController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = XxlJobAutoConfiguration.class)
@ConditionalOnClass(SpringHttpExecutorTransportController.class)
@ConditionalOnProperty(prefix = "xxl.job.executor", name = "transport", havingValue = "SPRING_HTTP", matchIfMissing = true)
public class XxlJobSpringHttpTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExecutorBiz executorBiz(XxlJobSpringExecutor xxlJobSpringExecutor) {
        return ExecutorTransportFactory.create(xxlJobSpringExecutor.getTransport());
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutorTransportDispatcher executorTransportDispatcher(XxlJobSpringExecutor xxlJobSpringExecutor,
                                                                   ExecutorBiz executorBiz) {
        return new ExecutorTransportDispatcher(executorBiz, xxlJobSpringExecutor.getAccessToken());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SpringHttpExecutorTransportController springHttpExecutorTransportController(
            ExecutorTransportDispatcher executorTransportDispatcher) {
        return new SpringHttpExecutorTransportController(executorTransportDispatcher);
    }
}
