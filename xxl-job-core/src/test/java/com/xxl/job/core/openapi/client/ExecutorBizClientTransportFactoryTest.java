package com.xxl.job.core.openapi.client;

import org.junit.Assert;
import org.junit.Test;

public class ExecutorBizClientTransportFactoryTest {

    @Test
    public void shouldMatchAndNormalizeHttpAddress() {
        ExecutorBizClientTransport transport = ExecutorBizClientTransportFactory.match("127.0.0.1:8081");
        Assert.assertEquals("HTTP", transport.type());
        Assert.assertEquals("http://127.0.0.1:8081/", transport.normalizeAddress("127.0.0.1:8081"));
        Assert.assertEquals("http://127.0.0.1:8081/", transport.normalizeAddress("http://127.0.0.1:8081"));
    }

    @Test
    public void shouldSupportExplicitTransportPrefix() {
        ExecutorBizEndpoint endpoint = ExecutorBizEndpointParser.parse("HTTP::127.0.0.1:8081");
        Assert.assertTrue(endpoint.hasExplicitTransport());
        Assert.assertEquals("HTTP", endpoint.getTransport());
        Assert.assertEquals("127.0.0.1:8081", endpoint.getAddress());
        Assert.assertEquals("HTTP", ExecutorBizClientTransportFactory.match(endpoint).type());
    }
}
