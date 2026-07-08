package com.xxl.job.core.spring.boot.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.xxl.job.core.context.XxlJobContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class XxlJobLogbackAppenderTest {

    @Test
    public void appenderShouldCaptureCurrentJobLogWithLimits() throws Exception {
        Path logFile = Files.createTempFile("xxl-job-log-capture", ".log");
        XxlJobContext.setXxlJobContext(new XxlJobContext(1, "", 2, System.currentTimeMillis(), logFile.toString(), 0, 1));

        XxlJobLogbackAppender appender = new XxlJobLogbackAppender();
        appender.setLevel("INFO");
        appender.setIncludePackages("com.example.");
        appender.setExcludePackages("com.example.noisy.");
        appender.setMaxEventLength(80);
        appender.setMaxEventsPerJob(1);
        appender.start();

        appender.doAppend(event(Level.DEBUG, "com.example.Job", "debug skipped"));
        appender.doAppend(event(Level.INFO, "com.other.Job", "package skipped"));
        appender.doAppend(event(Level.INFO, "com.example.noisy.Job", "exclude skipped"));
        appender.doAppend(event(Level.INFO, "com.example.Job", "captured " + "x".repeat(100)));
        appender.doAppend(event(Level.INFO, "com.example.Job", "over limit"));

        String content = Files.readString(logFile);
        Assert.assertTrue(content.contains("captured"));
        Assert.assertTrue(content.contains("[truncated]"));
        Assert.assertTrue(content.contains("reached max-events-per-job"));
        Assert.assertFalse(content.contains("debug skipped"));
        Assert.assertFalse(content.contains("package skipped"));
        Assert.assertFalse(content.contains("exclude skipped"));
    }

    private LoggingEvent event(Level level, String loggerName, String message) {
        ch.qos.logback.classic.LoggerContext loggerContext =
                (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
        LoggingEvent event = new LoggingEvent();
        event.setLevel(level);
        event.setLoggerName(loggerName);
        event.setLoggerContext(loggerContext);
        event.setLoggerContextRemoteView(loggerContext.getLoggerContextRemoteView());
        event.setThreadName("test-thread");
        event.setMessage(message);
        event.setTimeStamp(System.currentTimeMillis());
        event.setLoggerName(Logger.ROOT_LOGGER_NAME.equals(loggerName) ? Logger.ROOT_LOGGER_NAME : loggerName);
        return event;
    }
}
