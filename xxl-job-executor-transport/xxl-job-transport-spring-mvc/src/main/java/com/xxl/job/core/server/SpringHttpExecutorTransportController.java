package com.xxl.job.core.server;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class SpringHttpExecutorTransportController {

    private final ExecutorTransportDispatcher executorTransportDispatcher;

    public SpringHttpExecutorTransportController(ExecutorTransportDispatcher executorTransportDispatcher) {
        this.executorTransportDispatcher = executorTransportDispatcher;
    }

    @PostMapping("/beat")
    public Object beat(@RequestHeader(value = "XXL-JOB-ACCESS-TOKEN", required = false) String accessToken) {
        return executorTransportDispatcher.dispatch("POST", "/beat", null, accessToken);
    }

    @PostMapping("/idleBeat")
    public Object idleBeat(@RequestBody(required = false) String body,
                           @RequestHeader(value = "XXL-JOB-ACCESS-TOKEN", required = false) String accessToken) {
        return executorTransportDispatcher.dispatch("POST", "/idleBeat", body, accessToken);
    }

    @PostMapping("/run")
    public Object run(@RequestBody(required = false) String body,
                      @RequestHeader(value = "XXL-JOB-ACCESS-TOKEN", required = false) String accessToken) {
        return executorTransportDispatcher.dispatch("POST", "/run", body, accessToken);
    }

    @PostMapping("/kill")
    public Object kill(@RequestBody(required = false) String body,
                       @RequestHeader(value = "XXL-JOB-ACCESS-TOKEN", required = false) String accessToken) {
        return executorTransportDispatcher.dispatch("POST", "/kill", body, accessToken);
    }

    @PostMapping("/log")
    public Object log(@RequestBody(required = false) String body,
                      @RequestHeader(value = "XXL-JOB-ACCESS-TOKEN", required = false) String accessToken) {
        return executorTransportDispatcher.dispatch("POST", "/log", body, accessToken);
    }
}
