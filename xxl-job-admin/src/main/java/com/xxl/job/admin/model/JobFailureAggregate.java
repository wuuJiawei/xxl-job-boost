package com.xxl.job.admin.model;

import java.util.Date;

/**
 * 任务失败聚合视图
 */
public class JobFailureAggregate {

    private int jobGroup;
    private int jobId;
    private String jobDesc;
    private String author;
    private String jobTag;
    private int failureCount;
    private Date lastFailureTime;
    private long lastLogId;
    private int lastTriggerCode;
    private int lastHandleCode;
    private String lastTriggerMsg;
    private String lastHandleMsg;

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

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public Date getLastFailureTime() {
        return lastFailureTime;
    }

    public void setLastFailureTime(Date lastFailureTime) {
        this.lastFailureTime = lastFailureTime;
    }

    public long getLastLogId() {
        return lastLogId;
    }

    public void setLastLogId(long lastLogId) {
        this.lastLogId = lastLogId;
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

    public String getLastTriggerMsg() {
        return lastTriggerMsg;
    }

    public void setLastTriggerMsg(String lastTriggerMsg) {
        this.lastTriggerMsg = lastTriggerMsg;
    }

    public String getLastHandleMsg() {
        return lastHandleMsg;
    }

    public void setLastHandleMsg(String lastHandleMsg) {
        this.lastHandleMsg = lastHandleMsg;
    }
}
