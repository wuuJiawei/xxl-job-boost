package com.xxl.job.core.server;

import com.xxl.job.core.constant.Const;
import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.tool.error.ThrowableTool;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorTransportDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorTransportDispatcher.class);

    private final ExecutorBiz executorBiz;
    private final String accessToken;

    public ExecutorTransportDispatcher(ExecutorBiz executorBiz, String accessToken) {
        this.executorBiz = executorBiz;
        this.accessToken = accessToken;
    }

    public Object dispatch(String method, String uri, String requestData, String accessTokenReq) {
        if (!"POST".equalsIgnoreCase(method)) {
            return Response.ofFail("invalid request, HttpMethod not support.");
        }
        if (uri == null || uri.trim().isEmpty()) {
            return Response.ofFail("invalid request, uri-mapping empty.");
        }
        if (accessToken != null
                && !accessToken.trim().isEmpty()
                && !accessToken.equals(accessTokenReq)) {
            return Response.ofFail("The access token is wrong.");
        }

        try {
            switch (uri) {
                case "/beat":
                    return executorBiz.beat();
                case "/idleBeat":
                    return executorBiz.idleBeat(GsonTool.fromJson(requestData, IdleBeatRequest.class));
                case "/run":
                    return executorBiz.run(GsonTool.fromJson(requestData, TriggerRequest.class));
                case "/kill":
                    return executorBiz.kill(GsonTool.fromJson(requestData, KillRequest.class));
                case "/log":
                    return executorBiz.log(GsonTool.fromJson(requestData, LogRequest.class));
                default:
                    return Response.ofFail("invalid request, uri-mapping(" + uri + ") not found.");
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return Response.ofFail("request error:" + ThrowableTool.toString(e));
        }
    }

    public String getAccessTokenHeaderName() {
        return Const.XXL_JOB_ACCESS_TOKEN;
    }
}
