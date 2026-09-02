package com.xxl.job.admin.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyJobSyncServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void previewsLegacyGroupsAndJobsThroughCookieSession() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/xxl-job-admin/auth/doLogin", exchange -> respond(exchange, 404, ""));
        server.createContext("/xxl-job-admin/login", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "XXL_JOB_LOGIN_IDENTITY=test-token; Path=/");
            respond(exchange, 200, "{\"code\":200,\"msg\":null,\"data\":null}");
        });
        server.createContext("/xxl-job-admin/jobgroup/pageList", exchange -> {
            assertEquals("XXL_JOB_LOGIN_IDENTITY=test-token", exchange.getRequestHeaders().getFirst("Cookie"));
            respond(exchange, 200, "{\"code\":200,\"data\":{\"total\":1,\"data\":[{\"id\":7,\"appname\":\"legacy-executor\",\"title\":\"旧执行器\"}]}}");
        });
        server.createContext("/xxl-job-admin/jobinfo/pageList", exchange -> respond(exchange, 200,
                "{\"code\":200,\"data\":{\"total\":1,\"data\":[{\"id\":9,\"jobGroup\":7,\"jobDesc\":\"每日任务\",\"author\":\"ops\",\"scheduleType\":\"CRON\",\"scheduleConf\":\"0 0 2 * * ?\",\"misfireStrategy\":\"DO_NOTHING\",\"executorRouteStrategy\":\"FIRST\",\"executorHandler\":\"dailyJob\",\"executorBlockStrategy\":\"SERIAL_EXECUTION\",\"glueType\":\"BEAN\"}]}}"));
        server.start();

        LegacyJobSyncService service = new LegacyJobSyncService();
        Map<String, Object> preview = service.preview(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/xxl-job-admin/", "admin", "secret");

        List<Map<String, Object>> groups = (List<Map<String, Object>>) preview.get("groups");
        List<Map<String, Object>> jobs = (List<Map<String, Object>>) preview.get("jobs");
        assertEquals("legacy-executor", groups.get(0).get("appname"));
        assertEquals("dailyJob", jobs.get(0).get("executorHandler"));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        respond(exchange, status, body.getBytes(StandardCharsets.UTF_8));
    }

    private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
