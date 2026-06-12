package com.xxl.job.executor.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.impl.ExecutorBizImpl;
import com.xxl.job.core.server.ExecutorTransportDispatcher;
import com.xxl.job.core.server.SpringHttpExecutorTransportController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "xxl.job.executor.transport", havingValue = "SPRING_HTTP")
public class XxlJobTransportConfig {

    @Bean
    public ExecutorBiz executorBiz() {
        return new ExecutorBizImpl();
    }

    @Bean
    public ExecutorTransportDispatcher executorTransportDispatcher(XxlJobSpringExecutor xxlJobSpringExecutor,
                                                                   ExecutorBiz executorBiz) {
        return new ExecutorTransportDispatcher(executorBiz, xxlJobSpringExecutor.getAccessToken());
    }

    @Bean
    public SpringHttpExecutorTransportController springHttpExecutorTransportController(
            ExecutorTransportDispatcher executorTransportDispatcher) {
        return new SpringHttpExecutorTransportController(executorTransportDispatcher);
    }
}
