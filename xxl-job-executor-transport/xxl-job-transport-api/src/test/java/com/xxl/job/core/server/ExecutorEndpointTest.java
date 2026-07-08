package com.xxl.job.core.server;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutorEndpointTest {

    @Test
    public void shouldDeclareRequiredEndpointPaths() {
        Set<String> paths = Arrays.stream(ExecutorEndpoint.values())
                .map(ExecutorEndpoint::path)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Assert.assertEquals(ExecutorEndpoint.values().length, paths.size());
        Assert.assertTrue(paths.contains(ExecutorEndpointPaths.BEAT));
        Assert.assertTrue(paths.contains(ExecutorEndpointPaths.IDLE_BEAT));
        Assert.assertTrue(paths.contains(ExecutorEndpointPaths.RUN));
        Assert.assertTrue(paths.contains(ExecutorEndpointPaths.KILL));
        Assert.assertTrue(paths.contains(ExecutorEndpointPaths.LOG));
    }

    @Test
    public void shouldMatchEndpointByPath() {
        for (ExecutorEndpoint endpoint : ExecutorEndpoint.values()) {
            Assert.assertSame(endpoint, ExecutorEndpoint.fromPath(endpoint.path()));
        }
        Assert.assertNull(ExecutorEndpoint.fromPath(null));
        Assert.assertNull(ExecutorEndpoint.fromPath("/missing"));
    }
}
