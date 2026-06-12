package com.xxl.job.core.server;

public interface ExecutorTransport {

    void start(String address, int port, String appname, String accessToken) throws Exception;

    void stop() throws Exception;
}
