package com.xxl.job.core.openapi.client;

import com.xxl.job.core.openapi.ExecutorBiz;

public interface ExecutorBizClientTransport {

    String type();

    boolean supports(String address);

    String normalizeAddress(String address);

    ExecutorBiz create(String address, String accessToken, int timeout);
}
