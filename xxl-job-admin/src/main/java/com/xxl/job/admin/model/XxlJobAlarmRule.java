package com.xxl.job.admin.model;

import java.util.Date;

public class XxlJobAlarmRule {

    private int id;
    private String name;
    private int jobGroup;
    private Integer jobId;
    private String alarmEvent;
    private String channelIds;
    private int enabled;
    private String remark;
    private Date updateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(int jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getAlarmEvent() {
        return alarmEvent;
    }

    public void setAlarmEvent(String alarmEvent) {
        this.alarmEvent = alarmEvent;
    }

    public String getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(String channelIds) {
        this.channelIds = channelIds;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
