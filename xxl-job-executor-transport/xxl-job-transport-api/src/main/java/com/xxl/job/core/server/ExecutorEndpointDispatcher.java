package com.xxl.job.core.server;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.error.ThrowableTool;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorEndpointDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorEndpointDispatcher.class);

    private final ExecutorBiz executorBiz;
    private final String accessToken;

    public ExecutorEndpointDispatcher(ExecutorBiz executorBiz, String accessToken) {
        this.executorBiz = executorBiz;
        this.accessToken = accessToken;
    }

    public Object dispatch(String method, String path, String requestData, String accessTokenReq) {
        if (!"POST".equalsIgnoreCase(method)) {
            return Response.ofFail("invalid request, HttpMethod not support.");
        }
        if (StringTool.isBlank(path)) {
            return Response.ofFail("invalid request, uri-mapping empty.");
        }
        if (StringTool.isNotBlank(accessToken) && !accessToken.equals(accessTokenReq)) {
            return Response.ofFail("The access token is wrong.");
        }

        ExecutorEndpoint endpoint = ExecutorEndpoint.fromPath(path);
        if (endpoint == null) {
            return Response.ofFail("invalid request, uri-mapping(" + path + ") not found.");
        }

        try {
            return invoke(endpoint, requestData);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return Response.ofFail("request error:" + ThrowableTool.toString(e));
        }
    }

    protected Object invoke(ExecutorEndpoint endpoint, String requestData) {
        switch (endpoint) {
            case BEAT:
                return executorBiz.beat();
            case IDLE_BEAT:
                return executorBiz.idleBeat(GsonTool.fromJson(requestData, IdleBeatRequest.class));
            case RUN:
                return executorBiz.run(GsonTool.fromJson(requestData, TriggerRequest.class));
            case KILL:
                return executorBiz.kill(GsonTool.fromJson(requestData, KillRequest.class));
            case LOG:
                return executorBiz.log(GsonTool.fromJson(requestData, LogRequest.class));
            default:
                return Response.ofFail("unsupported executor endpoint:" + endpoint);
        }
    }

    public String getAccessTokenHeaderName() {
        return ExecutorEndpointHeaders.ACCESS_TOKEN;
    }
}
