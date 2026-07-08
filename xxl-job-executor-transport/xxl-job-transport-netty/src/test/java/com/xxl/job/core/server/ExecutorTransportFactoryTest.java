package com.xxl.job.core.server;

import org.junit.Assert;
import org.junit.Test;

public class ExecutorTransportFactoryTest {

    @Test
    public void shouldCreateDefaultTransport() {
        Assert.assertEquals("NETTY_EMBED", ExecutorTransportFactory.create(null).type());
    }

    @Test
    public void shouldExposeExecutorBizOperationsFromTransport() {
        ExecutorTransport transport = ExecutorTransportFactory.create("NETTY_EMBED");
        Assert.assertNotNull(transport.executorBiz());
        Assert.assertNotNull(transport.beat());
    }
}
