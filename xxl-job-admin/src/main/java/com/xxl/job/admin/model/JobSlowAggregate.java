package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 任务慢执行聚合视图
 */
public class JobSlowAggregate {

    private int jobGroup;
    private int jobId;
    private String jobDesc;
    private String author;
    private String jobTag;
    private int slowCount;
    private long avgDurationMs;
    private long maxDurationMs;
    private Date lastSlowTime;
    private long lastLogId;
    private long lastDurationMs;
    private int lastTriggerCode;
    private int lastHandleCode;

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
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

    public int getSlowCount() {
        return slowCount;
    }

    public void setSlowCount(int slowCount) {
        this.slowCount = slowCount;
    }

    public long getAvgDurationMs() {
        return avgDurationMs;
    }

    public void setAvgDurationMs(long avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }

    public long getMaxDurationMs() {
        return maxDurationMs;
    }

    public void setMaxDurationMs(long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }

    public Date getLastSlowTime() {
        return lastSlowTime;
    }

    public void setLastSlowTime(Date lastSlowTime) {
        this.lastSlowTime = lastSlowTime;
    }

    public long getLastLogId() {
        return lastLogId;
    }

    public void setLastLogId(long lastLogId) {
        this.lastLogId = lastLogId;
    }

    public long getLastDurationMs() {
        return lastDurationMs;
    }

    public void setLastDurationMs(long lastDurationMs) {
        this.lastDurationMs = lastDurationMs;
    }

    public int getLastTriggerCode() {
        return lastTriggerCode;
    }

    public void setLastTriggerCode(int lastTriggerCode) {
        this.lastTriggerCode = lastTriggerCode;
    }

    public int getLastHandleCode() {
        return lastHandleCode;
    }

    public void setLastHandleCode(int lastHandleCode) {
        this.lastHandleCode = lastHandleCode;
    }
}
