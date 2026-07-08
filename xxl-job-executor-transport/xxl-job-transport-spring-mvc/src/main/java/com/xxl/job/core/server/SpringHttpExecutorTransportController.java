package com.xxl.job.core.server;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class SpringHttpExecutorTransportController implements ExecutorEndpointAdapter {

    private final ExecutorEndpointDispatcher executorEndpointDispatcher;

    public SpringHttpExecutorTransportController(ExecutorEndpointDispatcher executorEndpointDispatcher) {
        this.executorEndpointDispatcher = executorEndpointDispatcher;
    }

    @PostMapping(ExecutorEndpointPaths.BEAT)
    public Object beat(@RequestHeader(value = ExecutorEndpointHeaders.ACCESS_TOKEN, required = false) String accessToken) {
        return executorEndpointDispatcher.dispatch("POST", ExecutorEndpointPaths.BEAT, null, accessToken);
    }

    @PostMapping(ExecutorEndpointPaths.IDLE_BEAT)
    public Object idleBeat(@RequestBody(required = false) String body,
                           @RequestHeader(value = ExecutorEndpointHeaders.ACCESS_TOKEN, required = false) String accessToken) {
        return executorEndpointDispatcher.dispatch("POST", ExecutorEndpointPaths.IDLE_BEAT, body, accessToken);
    }

    @PostMapping(ExecutorEndpointPaths.RUN)
    public Object run(@RequestBody(required = false) String body,
                      @RequestHeader(value = ExecutorEndpointHeaders.ACCESS_TOKEN, required = false) String accessToken) {
        return executorEndpointDispatcher.dispatch("POST", ExecutorEndpointPaths.RUN, body, accessToken);
    }

    @PostMapping(ExecutorEndpointPaths.KILL)
    public Object kill(@RequestBody(required = false) String body,
                       @RequestHeader(value = ExecutorEndpointHeaders.ACCESS_TOKEN, required = false) String accessToken) {
        return executorEndpointDispatcher.dispatch("POST", ExecutorEndpointPaths.KILL, body, accessToken);
    }

    @PostMapping(ExecutorEndpointPaths.LOG)
    public Object log(@RequestBody(required = false) String body,
                      @RequestHeader(value = ExecutorEndpointHeaders.ACCESS_TOKEN, required = false) String accessToken) {
        return executorEndpointDispatcher.dispatch("POST", ExecutorEndpointPaths.LOG, body, accessToken);
    }

    @Override
    public String framework() {
        return "Spring MVC";
    }

    @Override
    public Set<ExecutorEndpoint> supportedEndpoints() {
        return EnumSet.allOf(ExecutorEndpoint.class);
    }
}
