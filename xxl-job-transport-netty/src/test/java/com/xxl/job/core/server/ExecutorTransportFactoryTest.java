package com.xxl.job.core.server;

import org.junit.Assert;
import org.junit.Test;

public class ExecutorTransportFactoryTest {

    @Test
    public void shouldCreateDefaultTransport() {
        Assert.assertTrue(ExecutorTransportFactory.create(null) instanceof NettyEmbedExecutorTransport);
    }
}
