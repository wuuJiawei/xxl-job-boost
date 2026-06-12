package com.xxl.job.core.server;

import org.junit.Assert;
import org.junit.Test;

public class ExecutorTransportFactoryTest {

    @Test
    public void shouldCreateDefaultAndSpringHttpTransport() {
        Assert.assertTrue(ExecutorTransportFactory.create(null) instanceof NettyEmbedExecutorTransport);
        Assert.assertTrue(ExecutorTransportFactory.create("SPRING_HTTP") instanceof SpringHttpExecutorTransport);
    }
}
