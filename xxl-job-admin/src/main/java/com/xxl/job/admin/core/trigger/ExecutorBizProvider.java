package com.xxl.job.admin.core.trigger;

import com.xxl.job.core.openapi.ExecutorBiz;

public interface ExecutorBizProvider {

    ExecutorBiz getExecutorBiz(String address);
}
