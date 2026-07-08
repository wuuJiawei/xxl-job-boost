package com.xxl.job.core.constant;

import com.xxl.job.core.server.ExecutorEndpointHeaders;

/**
 * Created by xuxueli on 17/5/10.
 */
public class Const {

    // ---------------------- for openapi ----------------------

    /**
     * access token
     */
    public static final String XXL_JOB_ACCESS_TOKEN = ExecutorEndpointHeaders.ACCESS_TOKEN;


    // ---------------------- for registry ----------------------

    /**
     * registry beat interval, default 30s
     */
    public static final int BEAT_TIMEOUT = 30;

    /**
     * registry dead timeout, default 90s
     */
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

}
