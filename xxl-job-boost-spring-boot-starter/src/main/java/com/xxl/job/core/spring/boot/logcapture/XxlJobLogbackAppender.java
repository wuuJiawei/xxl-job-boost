package com.xxl.job.core.spring.boot.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.tool.core.DateTool;
import com.xxl.tool.core.StringTool;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class XxlJobLogbackAppender extends AppenderBase<ILoggingEvent> {
    static final String NAME = "XXL_JOB_LOG_CAPTURE";
    private static final String TRUNCATED = "...[truncated]";
    private static final String LIMIT_REACHED = "xxl-job log capture reached max-events-per-job, later events skipped.";

    private Level level = Level.INFO;
    private int maxEventLength = 4096;
    private int maxEventsPerJob = 2000;
    private List<String> includePackages = List.of();
    private List<String> excludePackages = List.of("org.springframework.", "spring.", "com.zaxxer.hikari.");
    private boolean includeMdc = true;

    @Override
    protected void append(ILoggingEvent event) {
        XxlJobContext context = XxlJobContext.getXxlJobContext();
        if (context == null || StringTool.isBlank(context.getLogFileName()) || !accept(event)) {
            return;
        }

        int count = context.incrementCapturedLogCount();
        if (count > maxEventsPerJob) {
            if (count == maxEventsPerJob + 1) {
                try {
                    XxlJobFileAppender.appendLog(context.getLogFileName(), LIMIT_REACHED);
                } catch (Exception ignored) {
                }
            }
            return;
        }

        try {
            XxlJobFileAppender.appendLog(context.getLogFileName(), format(event));
        } catch (Exception ignored) {
        }
    }

    private boolean accept(ILoggingEvent event) {
        if (event == null || !event.getLevel().isGreaterOrEqual(level)) {
            return false;
        }
        String loggerName = event.getLoggerName();
        if (!includePackages.isEmpty() && includePackages.stream().noneMatch(loggerName::startsWith)) {
            return false;
        }
        return excludePackages.stream().noneMatch(loggerName::startsWith);
    }

    private String format(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(DateTool.formatDateTime(new Date(event.getTimeStamp())))
                .append(" [").append(event.getThreadName()).append("] ")
                .append(event.getLevel()).append(" ")
                .append(event.getLoggerName()).append(" - ")
                .append(event.getFormattedMessage());

        if (includeMdc && event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            builder.append(" MDC=").append(event.getMDCPropertyMap());
        }
        appendThrowable(builder, event.getThrowableProxy());

        String log = builder.toString();
        if (maxEventLength > 0 && log.length() > maxEventLength) {
            return log.substring(0, maxEventLength) + TRUNCATED;
        }
        return log;
    }

    private void appendThrowable(StringBuilder builder, IThrowableProxy throwable) {
        if (throwable == null) {
            return;
        }
        builder.append(System.lineSeparator()).append(throwable.getClassName()).append(": ").append(throwable.getMessage());
        for (StackTraceElementProxy element : throwable.getStackTraceElementProxyArray()) {
            builder.append(System.lineSeparator()).append("\tat ").append(element);
        }
    }

    public void setLevel(String level) {
        this.level = Level.toLevel(level, Level.INFO);
    }

    public void setMaxEventLength(int maxEventLength) {
        this.maxEventLength = maxEventLength;
    }

    public void setMaxEventsPerJob(int maxEventsPerJob) {
        this.maxEventsPerJob = maxEventsPerJob > 0 ? maxEventsPerJob : Integer.MAX_VALUE;
    }

    public void setIncludePackages(String includePackages) {
        this.includePackages = split(includePackages);
    }

    public void setExcludePackages(String excludePackages) {
        this.excludePackages = split(excludePackages);
    }

    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    private List<String> split(String value) {
        if (StringTool.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringTool::isNotBlank)
                .toList();
    }
}
