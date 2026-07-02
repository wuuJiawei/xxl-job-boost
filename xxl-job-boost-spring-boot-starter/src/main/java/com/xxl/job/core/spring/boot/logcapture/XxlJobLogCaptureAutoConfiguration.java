package com.xxl.job.core.spring.boot.logcapture;

import com.xxl.job.core.spring.boot.XxlJobProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobLogCaptureAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "xxl.job.executor.log-capture", name = "enabled", havingValue = "true")
    public XxlJobLogCaptureRegistrar xxlJobLogCaptureRegistrar(XxlJobProperties properties) {
        return new XxlJobLogCaptureRegistrar(properties.getExecutor().getLogCapture());
    }
}
