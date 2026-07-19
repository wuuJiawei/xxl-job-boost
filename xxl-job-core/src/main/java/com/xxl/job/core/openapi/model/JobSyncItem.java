package com.xxl.job.core.openapi.model;

public class JobSyncItem {

    private String executorHandler;
    private String jobDesc;
    private String author;
    private String jobTag;
    private String alarmEmail;
    private String alarmChannelIds;
    private String alarmEventTypes;
    private String scheduleType;
    private String scheduleConf;
    private String executorParam;
    private String executorRouteStrategy;
    private String misfireStrategy;
    private String executorBlockStrategy;
    private int executorTimeout;
    private int executorFailRetryCount;
    private String startPolicy;

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getJobTag() {
        return jobTag;
    }

    public void setJobTag(String jobTag) {
        this.jobTag = jobTag;
    }

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public String getAlarmChannelIds() {
        return alarmChannelIds;
    }

    public void setAlarmChannelIds(String alarmChannelIds) {
        this.alarmChannelIds = alarmChannelIds;
    }

    public String getAlarmEventTypes() {
        return alarmEventTypes;
    }

    public void setAlarmEventTypes(String alarmEventTypes) {
        this.alarmEventTypes = alarmEventTypes;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(String misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public String getStartPolicy() {
        return startPolicy;
    }

    public void setStartPolicy(String startPolicy) {
        this.startPolicy = startPolicy;
    }
}
