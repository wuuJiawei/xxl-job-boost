package com.xxl.job.core.spring.boot;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.impl.ExecutorBizImpl;
import com.xxl.job.core.server.ExecutorTransportDispatcher;
import com.xxl.job.core.server.SpringHttpExecutorTransportController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "xxl.job.executor", name = "appname")
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties properties, Environment environment) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdmin().getAddresses());
        executor.setAccessToken(properties.getAdmin().getAccessToken());
        executor.setTimeout(properties.getAdmin().getTimeout());
        executor.setEnabled(properties.getExecutor().getEnabled());
        executor.setAppname(properties.getExecutor().getAppname());
        executor.setGroupTitle(properties.getExecutor().getGroupTitle());
        executor.setSyncMode(properties.getExecutor().getSyncMode());
        executor.setAddress(properties.getExecutor().getAddress());
        executor.setIp(properties.getExecutor().getIp());
        executor.setPort(resolveExecutorPort(properties, environment));
        executor.setTransport(properties.getExecutor().getTransport());
        executor.setLogPath(properties.getExecutor().getLogpath());
        executor.setLogRetentionDays(properties.getExecutor().getLogretentiondays());
        executor.setExcludedPackage(properties.getExecutor().getExcludedpackage());
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "xxl.job.executor", name = "transport", havingValue = "SPRING_HTTP")
    public ExecutorBiz executorBiz() {
        return new ExecutorBizImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "xxl.job.executor", name = "transport", havingValue = "SPRING_HTTP")
    public ExecutorTransportDispatcher executorTransportDispatcher(XxlJobSpringExecutor xxlJobSpringExecutor,
                                                                   ExecutorBiz executorBiz) {
        return new ExecutorTransportDispatcher(executorBiz, xxlJobSpringExecutor.getAccessToken());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "xxl.job.executor", name = "transport", havingValue = "SPRING_HTTP")
    public SpringHttpExecutorTransportController springHttpExecutorTransportController(
            ExecutorTransportDispatcher executorTransportDispatcher) {
        return new SpringHttpExecutorTransportController(executorTransportDispatcher);
    }

    private int resolveExecutorPort(XxlJobProperties properties, Environment environment) {
        if ("SPRING_HTTP".equalsIgnoreCase(properties.getExecutor().getTransport())) {
            Integer port = environment.getProperty("server.port", Integer.class);
            if (port != null) {
                return port;
            }
            return 8080;
        }
        return properties.getExecutor().getPort();
    }
}
