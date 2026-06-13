package com.xxl.job.admin.model;

import java.util.List;

/**
 * 治理总览视图
 */
public class GovernanceOverview {

    private int totalJobs;
    private int taggedJobs;
    private int ownedJobs;
    private int auditEvents;
    private List<JobFailureAggregate> failureTopList;
    private List<JobSlowAggregate> slowTopList;
    private List<XxlJobAuditLog> recentAuditList;

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public int getTaggedJobs() {
        return taggedJobs;
    }

    public void setTaggedJobs(int taggedJobs) {
        this.taggedJobs = taggedJobs;
    }

    public int getOwnedJobs() {
        return ownedJobs;
    }

    public void setOwnedJobs(int ownedJobs) {
        this.ownedJobs = ownedJobs;
    }

    public int getAuditEvents() {
        return auditEvents;
    }

    public void setAuditEvents(int auditEvents) {
        this.auditEvents = auditEvents;
    }

    public List<JobFailureAggregate> getFailureTopList() {
        return failureTopList;
    }

    public void setFailureTopList(List<JobFailureAggregate> failureTopList) {
        this.failureTopList = failureTopList;
    }

    public List<JobSlowAggregate> getSlowTopList() {
        return slowTopList;
    }

    public void setSlowTopList(List<JobSlowAggregate> slowTopList) {
        this.slowTopList = slowTopList;
    }

    public List<XxlJobAuditLog> getRecentAuditList() {
        return recentAuditList;
    }

    public void setRecentAuditList(List<XxlJobAuditLog> recentAuditList) {
        this.recentAuditList = recentAuditList;
    }
}
