package com.xxl.job.core.openapi;

import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.LogResult;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.tool.response.Response;

public interface ExecutorBiz {

    Response<String> beat();

    Response<String> idleBeat(IdleBeatRequest idleBeatRequest);

    Response<String> run(TriggerRequest triggerRequest);

    Response<String> kill(KillRequest killRequest);

    Response<LogResult> log(LogRequest logRequest);
}
