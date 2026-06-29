package com.xxl.job.core.server;

public class ExecutorTransportContext {
    private final String address;
    private final int port;
    private final String appname;
    private final String accessToken;

    public ExecutorTransportContext(String address, int port, String appname, String accessToken) {
        this.address = address;
        this.port = port;
        this.appname = appname;
        this.accessToken = accessToken;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getAppname() {
        return appname;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
