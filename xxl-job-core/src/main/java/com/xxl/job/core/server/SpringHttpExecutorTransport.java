package com.xxl.job.core.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringHttpExecutorTransport implements ExecutorTransport {
    private static final Logger logger = LoggerFactory.getLogger(SpringHttpExecutorTransport.class);

    private ExecutorTransportContext context;

    @Override
    public void start(String address, int port, String appname, String accessToken) {
        this.context = new ExecutorTransportContext(address, port, appname, accessToken);
        logger.info(">>>>>>>>>>> xxl-job transport ready, type:{}, address:{}, port:{}",
                ExecutorTransportType.SPRING_HTTP.name(), address, port);
    }

    @Override
    public void stop() {
        logger.info(">>>>>>>>>>> xxl-job transport stop, type:{}, address:{}",
                ExecutorTransportType.SPRING_HTTP.name(), context == null ? null : context.getAddress());
    }
}
