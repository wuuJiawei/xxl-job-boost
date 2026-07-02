package com.xxl.job.core.spring.boot.logcapture;

import ch.qos.logback.classic.LoggerContext;
import com.xxl.job.core.spring.boot.XxlJobProperties;
import org.slf4j.LoggerFactory;

public class XxlJobLogCaptureRegistrar {
    private final XxlJobProperties.LogCapture properties;
    private XxlJobLogbackAppender appender;

    public XxlJobLogCaptureRegistrar(XxlJobProperties.LogCapture properties) {
        this.properties = properties;
    }

    public void start() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            return;
        }
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (root.getAppender(XxlJobLogbackAppender.NAME) != null) {
            return;
        }

        appender = new XxlJobLogbackAppender();
        appender.setName(XxlJobLogbackAppender.NAME);
        appender.setContext(loggerContext);
        appender.setLevel(properties.getLevel());
        appender.setMaxEventLength(properties.getMaxEventLength());
        appender.setMaxEventsPerJob(properties.getMaxEventsPerJob());
        appender.setIncludePackages(properties.getIncludePackages());
        appender.setExcludePackages(properties.getExcludePackages());
        appender.setIncludeMdc(properties.isIncludeMdc());
        appender.start();
        root.addAppender(appender);
    }

    public void stop() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) || appender == null) {
            return;
        }
        loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).detachAppender(appender);
        appender.stop();
    }
}
