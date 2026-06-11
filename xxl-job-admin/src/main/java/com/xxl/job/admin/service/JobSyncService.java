package com.xxl.job.admin.service;

import com.xxl.job.core.openapi.model.JobSyncRequest;
import com.xxl.tool.response.Response;

public interface JobSyncService {

    Response<String> sync(JobSyncRequest request);
}
