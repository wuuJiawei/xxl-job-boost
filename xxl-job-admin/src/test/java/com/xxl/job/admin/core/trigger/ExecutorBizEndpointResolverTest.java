package com.xxl.job.admin.core.trigger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExecutorBizEndpointResolverTest {

    private final ExecutorBizEndpointResolver resolver = new ExecutorBizEndpointResolver();

    @Test
    void shouldSupportBareAndExplicitHttpEndpoint() {
        Assertions.assertTrue(resolver.supports("127.0.0.1:8081"));
        Assertions.assertTrue(resolver.supports("HTTP::127.0.0.1:8081"));
        Assertions.assertFalse(resolver.supports("UNKNOWN::127.0.0.1:8081"));
    }
}
