package com.xxl.job.core.server;

import com.xxl.job.core.thread.ExecutorRegistryThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringHttpExecutorTransport implements ExecutorTransport {
    private static final Logger logger = LoggerFactory.getLogger(SpringHttpExecutorTransport.class);

    private static volatile ExecutorTransportContext activeContext;
    private ExecutorTransportContext context;

    @Override
    public String type() {
        return "SPRING_HTTP";
    }

    @Override
    public void start(String address, int port, String appname, String accessToken) {
        this.context = new ExecutorTransportContext(address, port, appname, accessToken);
        activeContext = this.context;
        ExecutorRegistryThread.getInstance().start(appname, registryValue(address));
        logger.info(">>>>>>>>>>> xxl-job transport ready, type:{}, address:{}, port:{}",
                type(), address, port);
    }

    @Override
    public void stop() {
        ExecutorRegistryThread.getInstance().toStop();
        if (context != null && activeContext == context) {
            activeContext = null;
        }
        logger.info(">>>>>>>>>>> xxl-job transport stop, type:{}, address:{}",
                type(), context == null ? null : context.getAddress());
    }

    public static ExecutorTransportContext getActiveContext() {
        return activeContext;
    }
}
