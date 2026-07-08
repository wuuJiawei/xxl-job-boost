package com.xxl.job.core.server;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.LogResult;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import org.junit.Assert;
import org.junit.Test;

public class ExecutorEndpointDispatcherTest {

    @Test
    public void shouldDispatchAllExecutorEndpoints() {
        RecordingExecutorBiz executorBiz = new RecordingExecutorBiz();
        ExecutorEndpointDispatcher dispatcher = new ExecutorEndpointDispatcher(executorBiz, "token");

        dispatcher.dispatch("POST", ExecutorEndpointPaths.BEAT, null, "token");
        Assert.assertEquals(ExecutorEndpoint.BEAT, executorBiz.lastEndpoint);

        dispatcher.dispatch("POST", ExecutorEndpointPaths.IDLE_BEAT, GsonTool.toJson(new IdleBeatRequest(11)), "token");
        Assert.assertEquals(ExecutorEndpoint.IDLE_BEAT, executorBiz.lastEndpoint);
        Assert.assertEquals(11, executorBiz.idleBeatRequest.getJobId());

        TriggerRequest triggerRequest = new TriggerRequest();
        triggerRequest.setJobId(12);
        dispatcher.dispatch("POST", ExecutorEndpointPaths.RUN, GsonTool.toJson(triggerRequest), "token");
        Assert.assertEquals(ExecutorEndpoint.RUN, executorBiz.lastEndpoint);
        Assert.assertEquals(12, executorBiz.triggerRequest.getJobId());

        dispatcher.dispatch("POST", ExecutorEndpointPaths.KILL, GsonTool.toJson(new KillRequest(13)), "token");
        Assert.assertEquals(ExecutorEndpoint.KILL, executorBiz.lastEndpoint);
        Assert.assertEquals(13, executorBiz.killRequest.getJobId());

        dispatcher.dispatch("POST", ExecutorEndpointPaths.LOG, GsonTool.toJson(new LogRequest(100L, 14L, 0)), "token");
        Assert.assertEquals(ExecutorEndpoint.LOG, executorBiz.lastEndpoint);
        Assert.assertEquals(14L, executorBiz.logRequest.getLogId());
    }

    @Test
    public void shouldRejectInvalidRequestBeforeDispatch() {
        RecordingExecutorBiz executorBiz = new RecordingExecutorBiz();
        ExecutorEndpointDispatcher dispatcher = new ExecutorEndpointDispatcher(executorBiz, "token");

        Assert.assertFalse(((Response<?>) dispatcher.dispatch("GET", ExecutorEndpointPaths.BEAT, null, "token")).isSuccess());
        Assert.assertFalse(((Response<?>) dispatcher.dispatch("POST", "", null, "token")).isSuccess());
        Assert.assertFalse(((Response<?>) dispatcher.dispatch("POST", "/missing", null, "token")).isSuccess());
        Assert.assertFalse(((Response<?>) dispatcher.dispatch("POST", ExecutorEndpointPaths.BEAT, null, "wrong")).isSuccess());
        Assert.assertNull(executorBiz.lastEndpoint);
    }

    private static class RecordingExecutorBiz implements ExecutorBiz {
        private ExecutorEndpoint lastEndpoint;
        private IdleBeatRequest idleBeatRequest;
        private TriggerRequest triggerRequest;
        private KillRequest killRequest;
        private LogRequest logRequest;

        @Override
        public Response<String> beat() {
            lastEndpoint = ExecutorEndpoint.BEAT;
            return Response.ofSuccess();
        }

        @Override
        public Response<String> idleBeat(IdleBeatRequest idleBeatRequest) {
            lastEndpoint = ExecutorEndpoint.IDLE_BEAT;
            this.idleBeatRequest = idleBeatRequest;
            return Response.ofSuccess();
        }

        @Override
        public Response<String> run(TriggerRequest triggerRequest) {
            lastEndpoint = ExecutorEndpoint.RUN;
            this.triggerRequest = triggerRequest;
            return Response.ofSuccess();
        }

        @Override
        public Response<String> kill(KillRequest killRequest) {
            lastEndpoint = ExecutorEndpoint.KILL;
            this.killRequest = killRequest;
            return Response.ofSuccess();
        }

        @Override
        public Response<LogResult> log(LogRequest logRequest) {
            lastEndpoint = ExecutorEndpoint.LOG;
            this.logRequest = logRequest;
            return Response.ofSuccess(new LogResult());
        }
    }
}
