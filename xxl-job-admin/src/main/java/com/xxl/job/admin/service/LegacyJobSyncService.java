package com.xxl.job.admin.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.util.JobGroupPermissionUtil;
import com.xxl.sso.core.model.LoginInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LegacyJobSyncService {
    private static final int PAGE_SIZE = 10_000;
    private static final String DEFAULT_ROUTE = "FIRST";
    private static final String DEFAULT_BLOCK = "SERIAL_EXECUTION";
    private static final String DEFAULT_MISFIRE = "DO_NOTHING";
    private static final String DEFAULT_GLUE = "BEAN";

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    private AuditLogService auditLogService;

    public Map<String, Object> preview(String sourceUrl, String username, String password) throws IOException, InterruptedException {
        RemoteData remoteData = loadRemoteData(sourceUrl, username, password);
        List<Map<String, Object>> groups = new ArrayList<>();
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (RemoteGroup group : remoteData.groups()) {
            groups.add(group.view());
            for (RemoteJob job : group.jobs()) {
                jobs.add(job.view(group));
            }
        }
        return Map.of("groups", groups, "jobs", jobs);
    }

    @Transactional
    public Map<String, Object> importJobs(String sourceUrl, String username, String password, List<Integer> selectedJobIds,
                                          LoginInfo loginInfo, jakarta.servlet.http.HttpServletRequest request)
            throws IOException, InterruptedException {
        if (selectedJobIds == null || selectedJobIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个任务");
        }
        RemoteData remoteData = loadRemoteData(sourceUrl, username, password);
        Set<Integer> selected = new HashSet<>(selectedJobIds);
        Map<Integer, RemoteJob> jobsById = new HashMap<>();
        Map<Integer, RemoteGroup> groupsById = new HashMap<>();
        Map<String, XxlJobGroup> localGroups = new HashMap<>();
        int groupsCreated = 0;

        Set<Integer> selectedGroupIds = new HashSet<>();
        for (RemoteGroup remoteGroup : remoteData.groups()) {
            groupsById.put(remoteGroup.id(), remoteGroup);
            for (RemoteJob job : remoteGroup.jobs()) {
                jobsById.put(job.id(), job);
                if (selected.contains(job.id())) {
                    selectedGroupIds.add(remoteGroup.id());
                }
            }
        }

        for (RemoteGroup remoteGroup : remoteData.groups()) {
            if (!selectedGroupIds.contains(remoteGroup.id())) {
                continue;
            }
            XxlJobGroup localGroup = xxlJobGroupMapper.loadByAppname(remoteGroup.appname());
            if (localGroup == null) {
                localGroup = new XxlJobGroup();
                localGroup.setAppname(remoteGroup.appname());
                localGroup.setTitle(remoteGroup.title());
                localGroup.setAddressType(0);
                localGroup.setUpdateTime(new Date());
                xxlJobGroupMapper.save(localGroup);
                groupsCreated++;
            }
            localGroups.put(remoteGroup.appname(), localGroup);
        }

        Map<Integer, Integer> sourceJobToLocalJob = new HashMap<>();
        List<PendingJob> pendingJobs = new ArrayList<>();
        int jobsSkipped = 0;
        for (Integer selectedId : selected) {
            RemoteJob remoteJob = jobsById.get(selectedId);
            if (remoteJob == null) {
                continue;
            }
            RemoteGroup remoteGroup = groupsById.get(remoteJob.groupId());
            XxlJobGroup localGroup = localGroups.get(remoteGroup.appname());
            if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, localGroup.getId())) {
                throw new IllegalArgumentException("没有目标执行器的导入权限：" + localGroup.getAppname());
            }
            XxlJobInfo existing = xxlJobInfoMapper.loadByGroupAndHandlerOrDesc(
                    localGroup.getId(), remoteJob.executorHandler(), remoteJob.jobDesc());
            if (existing != null) {
                sourceJobToLocalJob.put(remoteJob.id(), existing.getId());
                jobsSkipped++;
                continue;
            }
            XxlJobInfo localJob = remoteJob.toLocalJob(localGroup.getId());
            xxlJobInfoMapper.save(localJob);
            sourceJobToLocalJob.put(remoteJob.id(), localJob.getId());
            pendingJobs.add(new PendingJob(remoteJob, localJob));
        }

        for (PendingJob pending : pendingJobs) {
            String mappedChildren = mapChildJobs(pending.remoteJob().childJobId(), sourceJobToLocalJob);
            if (!mappedChildren.isEmpty()) {
                pending.localJob().setChildJobId(mappedChildren);
                pending.localJob().setUpdateTime(new Date());
                xxlJobInfoMapper.update(pending.localJob());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupsCreated", groupsCreated);
        result.put("jobsCreated", pendingJobs.size());
        result.put("jobsSkipped", jobsSkipped);
        result.put("warning", "导入任务默认停用，且不会覆盖已有任务；请确认新旧执行器接入和切换窗口后，再手动启动新任务。");
        auditLogService.record(loginInfo, request, "legacy-job-import", "job", null, "旧平台任务同步", null, result);
        return result;
    }

    private RemoteData loadRemoteData(String sourceUrl, String username, String password) throws IOException, InterruptedException {
        URI baseUri = validateBaseUri(sourceUrl);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String cookie = login(client, baseUri, username, password);
        JsonArray groupData = pageData(client, baseUri, cookie, "/jobgroup/pageList?offset=0&pagesize=" + PAGE_SIZE + "&appname=&title=");
        List<RemoteGroup> groups = new ArrayList<>();
        for (JsonElement element : groupData) {
            JsonObject object = element.getAsJsonObject();
            int groupId = intValue(object, "id", 0);
            String appname = stringValue(object, "appname", "");
            String title = stringValue(object, "title", stringValue(object, "name", appname));
            if (groupId > 0 && !appname.isBlank()) {
                JsonArray remoteJobs = loadJobs(client, baseUri, cookie, groupId);
                List<RemoteJob> jobs = new ArrayList<>();
                for (JsonElement job : remoteJobs) {
                    jobs.add(RemoteJob.fromJson(job.getAsJsonObject(), groupId));
                }
                groups.add(new RemoteGroup(groupId, appname, title, jobs));
            }
        }
        return new RemoteData(groups);
    }

    private JsonArray loadJobs(HttpClient client, URI baseUri, String cookie, int groupId) throws IOException, InterruptedException {
        String query = "offset=0&pagesize=" + PAGE_SIZE + "&jobGroup=" + groupId + "&jobId=0&triggerStatus=-1&jobDesc=&executorHandler=&author=&jobTag=";
        try {
            return pageData(client, baseUri, cookie, "/jobinfo/pageList?" + query);
        } catch (IllegalArgumentException firstFailure) {
            return pageData(client, baseUri, cookie, "/jobinfo/pageList?offset=0&pagesize=" + PAGE_SIZE
                    + "&jobGroup=" + groupId + "&triggerStatus=-1&name=&executorHandler=&author=");
        }
    }

    private String login(HttpClient client, URI baseUri, String username, String password) throws IOException, InterruptedException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("旧平台账号和密码不能为空");
        }
        String form = form("userName", username) + "&" + form("password", password) + "&ifRemember=on";
        HttpResponse<String> response = loginAt(client, baseUri.resolve("auth/doLogin"), form);
        if (response.statusCode() == 404 || response.statusCode() == 405) {
            response = loginAt(client, baseUri.resolve("login"), form);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300 || code(response.body()) != 200) {
            throw new IllegalArgumentException("旧平台登录失败，请检查地址、账号和密码");
        }
        return response.headers().allValues("set-cookie").stream()
                .map(value -> value.substring(0, value.indexOf(';') >= 0 ? value.indexOf(';') : value.length()))
                .reduce((left, right) -> left + "; " + right)
                .orElseThrow(() -> new IllegalArgumentException("旧平台登录成功但未返回会话 Cookie"));
    }

    private HttpResponse<String> loginAt(HttpClient client, URI uri, String form) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonArray pageData(HttpClient client, URI baseUri, String cookie, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path.substring(1)))
                .timeout(Duration.ofSeconds(30))
                .header("Cookie", cookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("旧平台接口访问失败，HTTP " + response.statusCode());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.get("code").getAsInt() != 200) {
            throw new IllegalArgumentException(root.has("msg") ? root.get("msg").getAsString() : "旧平台接口返回失败");
        }
        JsonElement data = root.get("data");
        if (data != null && data.isJsonObject() && data.getAsJsonObject().has("data")) {
            return data.getAsJsonObject().getAsJsonArray("data");
        }
        return data != null && data.isJsonArray() ? data.getAsJsonArray() : new JsonArray();
    }

    private URI validateBaseUri(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("旧平台地址不能为空");
        }
        String normalized = sourceUrl.trim();
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        URI uri = URI.create(normalized);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalArgumentException("旧平台地址必须是完整的 http 或 https 地址");
        }
        return uri;
    }

    private String form(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static int code(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        return root.has("code") ? root.get("code").getAsInt() : -1;
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsInt() : fallback;
    }

    private String mapChildJobs(String childJobId, Map<Integer, Integer> sourceJobToLocalJob) {
        if (childJobId == null || childJobId.isBlank()) {
            return "";
        }
        List<String> mapped = new ArrayList<>();
        for (String sourceId : childJobId.split(",")) {
            try {
                Integer localId = sourceJobToLocalJob.get(Integer.parseInt(sourceId.trim()));
                if (localId != null) {
                    mapped.add(String.valueOf(localId));
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed legacy child IDs instead of blocking unrelated imports.
            }
        }
        return String.join(",", mapped);
    }

    private record PendingJob(RemoteJob remoteJob, XxlJobInfo localJob) {
    }

    private record RemoteData(List<RemoteGroup> groups) {
    }

    private record RemoteGroup(int id, String appname, String title, List<RemoteJob> jobs) {
        private Map<String, Object> view() {
            return Map.of("id", id, "appname", appname, "title", title, "jobCount", jobs.size());
        }
    }

    private record RemoteJob(int id, int groupId, String jobDesc, String author, String alarmEmail,
                             String scheduleType, String scheduleConf, String misfireStrategy,
                             String executorRouteStrategy, String executorHandler, String executorParam,
                             String executorBlockStrategy, int executorTimeout, int executorFailRetryCount,
                             String glueType, String glueSource, String glueRemark, String childJobId) {
        private Map<String, Object> view(RemoteGroup group) {
            return Map.of("id", id, "groupId", group.id(), "appname", group.appname(), "groupTitle", group.title(),
                    "jobDesc", jobDesc, "author", author, "scheduleType", scheduleType, "scheduleConf", scheduleConf,
                    "executorHandler", executorHandler, "executorParam", executorParam == null ? "" : executorParam);
        }

        private XxlJobInfo toLocalJob(int localGroupId) {
            XxlJobInfo job = new XxlJobInfo();
            job.setJobGroup(localGroupId);
            job.setJobDesc(jobDesc);
            job.setAuthor(author == null || author.isBlank() ? "legacy-sync" : author);
            job.setAlarmEmail(alarmEmail == null ? "" : alarmEmail);
            job.setScheduleType(scheduleType == null || scheduleType.isBlank() ? "NONE" : scheduleType);
            job.setScheduleConf(scheduleConf == null ? "" : scheduleConf);
            job.setMisfireStrategy(misfireStrategy == null || misfireStrategy.isBlank() ? DEFAULT_MISFIRE : misfireStrategy);
            job.setExecutorRouteStrategy(executorRouteStrategy == null || executorRouteStrategy.isBlank() ? DEFAULT_ROUTE : executorRouteStrategy);
            job.setExecutorHandler(executorHandler == null ? "" : executorHandler);
            job.setExecutorParam(executorParam == null ? "" : executorParam);
            job.setExecutorBlockStrategy(executorBlockStrategy == null || executorBlockStrategy.isBlank() ? DEFAULT_BLOCK : executorBlockStrategy);
            job.setExecutorTimeout(executorTimeout);
            job.setExecutorFailRetryCount(executorFailRetryCount);
            job.setGlueType(glueType == null || glueType.isBlank() ? DEFAULT_GLUE : glueType);
            job.setGlueSource(glueSource == null ? "" : glueSource);
            job.setGlueRemark(glueRemark == null || glueRemark.isBlank() ? "legacy-sync" : glueRemark);
            job.setChildJobId("");
            job.setTriggerStatus(0);
            job.setAddTime(new Date());
            job.setUpdateTime(new Date());
            job.setGlueUpdatetime(new Date());
            return job;
        }

        private static RemoteJob fromJson(JsonObject object, int groupId) {
            return new RemoteJob(intValue(object, "id", 0), groupId,
                    stringValue(object, "jobDesc", stringValue(object, "name", "")),
                    stringValue(object, "author", "legacy-sync"), stringValue(object, "alarmEmail", ""),
                    stringValue(object, "scheduleType", "NONE"), stringValue(object, "scheduleConf", ""),
                    stringValue(object, "misfireStrategy", DEFAULT_MISFIRE), stringValue(object, "executorRouteStrategy", DEFAULT_ROUTE),
                    stringValue(object, "executorHandler", ""), stringValue(object, "executorParam", ""),
                    stringValue(object, "executorBlockStrategy", DEFAULT_BLOCK), intValue(object, "executorTimeout", 0),
                    intValue(object, "executorFailRetryCount", 0), stringValue(object, "glueType", DEFAULT_GLUE),
                    stringValue(object, "glueSource", ""), stringValue(object, "glueRemark", "legacy-sync"),
                    stringValue(object, "childJobId", stringValue(object, "childJobid", "")));
        }
    }
}
