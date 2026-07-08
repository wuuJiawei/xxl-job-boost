package com.xxl.job.core.server;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.client.ExecutorBizClientTransportFactory;
import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.LogResult;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.tool.response.Response;

public interface ExecutorTransport extends ExecutorBiz {

    String type();

    ExecutorBiz executorBiz();

    void start(String address, int port, String appname, String accessToken) throws Exception;

    void stop() throws Exception;

    default String registryValue(String address) {
        return ExecutorBizClientTransportFactory.normalizeEndpoint(address);
    }

    @Override
    default Response<String> beat() {
        return executorBiz().beat();
    }

    @Override
    default Response<String> idleBeat(IdleBeatRequest idleBeatRequest) {
        return executorBiz().idleBeat(idleBeatRequest);
    }

    @Override
    default Response<String> run(TriggerRequest triggerRequest) {
        return executorBiz().run(triggerRequest);
    }

    @Override
    default Response<String> kill(KillRequest killRequest) {
        return executorBiz().kill(killRequest);
    }

    @Override
    default Response<LogResult> log(LogRequest logRequest) {
        return executorBiz().log(logRequest);
    }
}
