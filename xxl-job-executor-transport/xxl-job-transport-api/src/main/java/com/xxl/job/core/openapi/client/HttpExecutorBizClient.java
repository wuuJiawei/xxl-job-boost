package com.xxl.job.core.openapi.client;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.model.IdleBeatRequest;
import com.xxl.job.core.openapi.model.KillRequest;
import com.xxl.job.core.openapi.model.LogRequest;
import com.xxl.job.core.openapi.model.LogResult;
import com.xxl.job.core.openapi.model.TriggerRequest;
import com.xxl.job.core.server.ExecutorEndpoint;
import com.xxl.job.core.server.ExecutorEndpointHeaders;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpExecutorBizClient implements ExecutorBiz {

    private final String addressUrl;
    private final String accessToken;
    private final int timeout;
    private final HttpClient httpClient;

    public HttpExecutorBizClient(String addressUrl, String accessToken, int timeout) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public Response<String> beat() {
        return post(ExecutorEndpoint.BEAT, null, String.class);
    }

    @Override
    public Response<String> idleBeat(IdleBeatRequest idleBeatRequest) {
        return post(ExecutorEndpoint.IDLE_BEAT, idleBeatRequest, String.class);
    }

    @Override
    public Response<String> run(TriggerRequest triggerRequest) {
        return post(ExecutorEndpoint.RUN, triggerRequest, String.class);
    }

    @Override
    public Response<String> kill(KillRequest killRequest) {
        return post(ExecutorEndpoint.KILL, killRequest, String.class);
    }

    @Override
    public Response<LogResult> log(LogRequest logRequest) {
        return execute(ExecutorEndpoint.LOG, logRequest, Response.class, LogResult.class);
    }

    private <T> Response<T> post(ExecutorEndpoint endpoint, Object requestBody, Class<T> responseType) {
        return execute(endpoint, requestBody, Response.class, responseType);
    }

    @SuppressWarnings("unchecked")
    private <T> T execute(ExecutorEndpoint endpoint, Object requestBody, Class<T> rawType, java.lang.reflect.Type... actualTypeArguments) {
        String finalUrl = endpointUrl(endpoint);
        String requestJson = requestBody == null ? "" : GsonTool.toJson(requestBody);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl))
                .timeout(Duration.ofSeconds(timeout));
        if (accessToken != null) {
            requestBuilder.header(ExecutorEndpointHeaders.ACCESS_TOKEN, accessToken);
        }

        if (requestJson.isEmpty()) {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder.header("Content-Type", "application/json; charset=UTF-8");
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestJson));
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            return (T) GsonTool.fromJson(response.body(), rawType, actualTypeArguments);
        } catch (Exception e) {
            throw new RuntimeException("Http Request Error (" + e.getMessage() + "). for url : " + finalUrl, e);
        }
    }

    private String endpointUrl(ExecutorEndpoint endpoint) {
        String finalAddress = addressUrl.endsWith("/")
                ? addressUrl.substring(0, addressUrl.length() - 1)
                : addressUrl;
        return finalAddress + endpoint.path();
    }
}
