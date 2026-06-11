package com.xxl.job.core.openapi.model;

import java.util.List;

public class JobSyncRequest {

    private String appname;
    private String groupTitle;
    private int addressType;
    private String syncMode;
    private List<JobSyncItem> jobs;

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public int getAddressType() {
        return addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public List<JobSyncItem> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobSyncItem> jobs) {
        this.jobs = jobs;
    }
}
