package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.mapper.XxlJobAlarmChannelMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRecordMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRuleMapper;
import com.xxl.job.admin.model.XxlJobAlarmChannel;
import com.xxl.job.admin.model.XxlJobAlarmRecord;
import com.xxl.job.admin.model.XxlJobAlarmRule;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.json.GsonTool;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlarmChannelService {
    private static final Logger logger = LoggerFactory.getLogger(AlarmChannelService.class);

    @Resource
    private XxlJobAlarmChannelMapper xxlJobAlarmChannelMapper;
    @Resource
    private XxlJobAlarmRecordMapper xxlJobAlarmRecordMapper;
    @Resource
    private XxlJobAlarmRuleMapper xxlJobAlarmRuleMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<Integer> normalizeChannelIds(String alarmChannelIds) {
        if (StringTool.isBlank(alarmChannelIds)) {
            return java.util.Collections.emptyList();
        }

        List<Integer> result = new ArrayList<>();
        Set<Integer> unique = new java.util.LinkedHashSet<>();
        for (String item : alarmChannelIds.split(",")) {
            String trimmed = item == null ? "" : item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!StringTool.isNumeric(trimmed)) {
                throw new IllegalArgumentException("告警渠道ID非法: " + trimmed);
            }
            unique.add(Integer.parseInt(trimmed));
        }
        result.addAll(unique);
        return result;
    }

    public String normalizeChannelIdsToString(String alarmChannelIds) {
        List<Integer> ids = normalizeChannelIds(alarmChannelIds);
        if (ids.isEmpty()) {
            return "";
        }

        List<XxlJobAlarmChannel> channels = xxlJobAlarmChannelMapper.findByIds(ids);
        Set<Integer> existingIds = channels.stream().map(XxlJobAlarmChannel::getId).collect(Collectors.toSet());
        for (Integer id : ids) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException("告警渠道不存在: " + id);
            }
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public List<AlarmEventType> normalizeEventTypes(String alarmEventTypes) {
        if (StringTool.isBlank(alarmEventTypes)) {
            return java.util.Collections.emptyList();
        }

        List<AlarmEventType> result = new ArrayList<>();
        Set<AlarmEventType> unique = EnumSet.noneOf(AlarmEventType.class);
        for (String item : alarmEventTypes.split(",")) {
            AlarmEventType eventType = AlarmEventType.match(item);
            if (eventType == null) {
                throw new IllegalArgumentException("告警事件类型非法: " + item);
            }
            unique.add(eventType);
        }
        result.addAll(unique);
        return result;
    }

    public String normalizeEventTypesToString(String alarmEventTypes) {
        List<AlarmEventType> types = normalizeEventTypes(alarmEventTypes);
        if (types.isEmpty()) {
            return "";
        }
        return types.stream().map(AlarmEventType::name).collect(Collectors.joining(","));
    }

    public boolean shouldAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        AlarmEventType currentEvent = currentEvent(jobLog);
        if (currentEvent == null) {
            return false;
        }

        List<AlarmEventType> configured = normalizeEventTypes(info.getAlarmEventTypes());
        if (configured.isEmpty()) {
            return true;
        }
        return configured.contains(currentEvent);
    }

    public boolean sendBoundChannels(XxlJobInfo info, XxlJobLog jobLog) {
        AlarmEventType alarmEvent = currentEvent(jobLog);
        if (alarmEvent == null || !shouldAlarm(info, jobLog)) {
            return true;
        }

        List<Integer> ids = normalizeChannelIds(info.getAlarmChannelIds());
        if (ids.isEmpty()) {
            return true;
        }

        List<XxlJobAlarmChannel> channels = xxlJobAlarmChannelMapper.findByIds(ids);
        Map<Integer, XxlJobAlarmChannel> channelMap = channels.stream()
                .collect(Collectors.toMap(XxlJobAlarmChannel::getId, item -> item));

        boolean success = true;
        for (Integer channelId : ids) {
            XxlJobAlarmChannel channel = channelMap.get(channelId);
            if (channel == null || channel.getEnabled() != 1) {
                persistRecord(info, jobLog, channel, "N/A", AlarmContentHelper.buildTitle(info),
                        AlarmContentHelper.buildHtmlContent(info, jobLog), alarmEvent,
                        AlarmDeliveryResult.fail(null, null, channel == null ? "渠道不存在" : "渠道已停用"));
                success = false;
                continue;
            }

            AlarmDeliveryResult result = sendByChannel(channel, info, jobLog, alarmEvent);
            if (!result.isSuccess()) {
                success = false;
            }
        }
        return success;
    }

    public boolean sendMatchedRules(XxlJobInfo info, XxlJobLog jobLog) {
        AlarmEventType alarmEvent = currentEvent(jobLog);
        if (alarmEvent == null || info == null) {
            return true;
        }

        List<XxlJobAlarmRule> rules = xxlJobAlarmRuleMapper.findMatched(info.getJobGroup(), info.getId(), alarmEvent.name());
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        boolean success = true;
        for (XxlJobAlarmRule rule : rules) {
            List<Integer> ids;
            try {
                ids = normalizeChannelIds(rule.getChannelIds());
            } catch (IllegalArgumentException e) {
                persistRuleRecord(info, jobLog, rule, null, alarmEvent,
                        AlarmDeliveryResult.fail(null, null, e.getMessage()));
                success = false;
                continue;
            }
            if (ids.isEmpty()) {
                continue;
            }

            List<XxlJobAlarmChannel> channels = xxlJobAlarmChannelMapper.findByIds(ids);
            Map<Integer, XxlJobAlarmChannel> channelMap = channels.stream()
                    .collect(Collectors.toMap(XxlJobAlarmChannel::getId, item -> item));

            for (Integer channelId : ids) {
                XxlJobAlarmChannel channel = channelMap.get(channelId);
                if (channel == null || channel.getEnabled() != 1) {
                    persistRuleRecord(info, jobLog, rule, channel, alarmEvent,
                            AlarmDeliveryResult.fail(null, null, channel == null ? "规则渠道不存在" : "规则渠道已停用"));
                    success = false;
                    continue;
                }

                AlarmDeliveryResult result = sendByChannel(channel, info, jobLog, alarmEvent);
                if (!result.isSuccess()) {
                    success = false;
                }
            }
        }
        return success;
    }

    public void recordLegacyEmail(XxlJobInfo info, XxlJobLog jobLog, String target, String title, String content, AlarmDeliveryResult result) {
        AlarmEventType alarmEvent = currentEvent(jobLog);
        if (alarmEvent == null || !shouldAlarm(info, jobLog)) {
            return;
        }
        persistRecord(info, jobLog, null, target, title, content, alarmEvent, result);
    }

    private AlarmDeliveryResult sendByChannel(XxlJobAlarmChannel channel, XxlJobInfo info, XxlJobLog jobLog, AlarmEventType alarmEvent) {
        AlarmChannelType type = AlarmChannelType.match(channel.getType());
        if (type == null) {
            AlarmDeliveryResult result = AlarmDeliveryResult.fail(null, null, "不支持的渠道类型");
            persistRecord(info, jobLog, channel, targetOf(channel), AlarmContentHelper.buildTitle(info),
                    AlarmContentHelper.buildHtmlContent(info, jobLog), alarmEvent, result);
            return result;
        }

        String title = AlarmContentHelper.buildTitle(info);
        String htmlContent = AlarmContentHelper.buildHtmlContent(info, jobLog);
        String textContent = AlarmContentHelper.buildPlainTextContent(info, jobLog);

        AlarmDeliveryResult result;
        try {
            result = switch (type) {
                case EMAIL -> sendEmailWebhook(channel, title, htmlContent, textContent);
                case WEBHOOK -> sendWebhook(channel, genericWebhookPayload(title, textContent));
                case FEISHU -> sendWebhook(channel, feishuPayload(title, textContent));
                case WECOM -> sendWebhook(channel, wecomPayload(title, textContent));
                case DINGTALK -> sendWebhook(channel, dingtalkPayload(title, textContent));
            };
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> xxl-job, channel alarm send error, channelId:{}, jobLogId:{}",
                    channel.getId(), jobLog.getId(), e);
            result = AlarmDeliveryResult.fail(null, null, e.getMessage());
        }

        persistRecord(info, jobLog, channel, targetOf(channel), title, htmlContent, alarmEvent, result);
        return result;
    }

    private AlarmDeliveryResult sendEmailWebhook(XxlJobAlarmChannel channel, String title, String htmlContent, String textContent)
            throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("content", htmlContent);
        payload.put("text", textContent);
        payload.put("recipients", splitRecipients(channel.getRecipients()));
        return sendWebhook(channel, payload);
    }

    private AlarmDeliveryResult sendWebhook(XxlJobAlarmChannel channel, Map<String, Object> payload)
            throws IOException, InterruptedException {
        if (StringTool.isBlank(channel.getEndpoint())) {
            return AlarmDeliveryResult.fail(null, null, "Webhook地址不能为空");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(channel.getEndpoint().trim()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(GsonTool.toJson(payload), StandardCharsets.UTF_8));

        Map<String, Object> extraHeaders = parseHeaders(channel.getHeadersJson());
        for (Map.Entry<String, Object> entry : extraHeaders.entrySet()) {
            if (entry.getValue() != null) {
                builder.header(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return AlarmDeliveryResult.success(statusCode, response.body());
        }
        return AlarmDeliveryResult.fail(statusCode, response.body(), "HTTP " + statusCode);
    }

    private Map<String, Object> genericWebhookPayload(String title, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("content", content);
        return payload;
    }

    private Map<String, Object> feishuPayload(String title, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "text");
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("text", title + "\n" + content);
        payload.put("content", text);
        return payload;
    }

    private Map<String, Object> wecomPayload(String title, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", "## " + title + "\n" + content.replace("\n", "\n> "));
        payload.put("markdown", markdown);
        return payload;
    }

    private Map<String, Object> dingtalkPayload(String title, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("title", title);
        markdown.put("text", "### " + title + "\n\n" + content);
        payload.put("markdown", markdown);
        return payload;
    }

    private Map<String, Object> parseHeaders(String headersJson) {
        if (StringTool.isBlank(headersJson)) {
            return java.util.Collections.emptyMap();
        }
        try {
            Map<String, Object> parsed = GsonTool.fromJsonMap(headersJson, String.class, Object.class);
            return parsed == null ? java.util.Collections.emptyMap() : parsed;
        } catch (Exception e) {
            logger.warn(">>>>>>>>>>> xxl-job, parse headersJson failed: {}", headersJson, e);
            return java.util.Collections.emptyMap();
        }
    }

    private List<String> splitRecipients(String recipients) {
        if (StringTool.isBlank(recipients)) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private String targetOf(XxlJobAlarmChannel channel) {
        if (channel == null) {
            return null;
        }
        if (StringTool.isNotBlank(channel.getRecipients())) {
            return channel.getRecipients();
        }
        return channel.getEndpoint();
    }

    private void persistRecord(XxlJobInfo info,
                               XxlJobLog jobLog,
                               XxlJobAlarmChannel channel,
                               String target,
                               String title,
                               String content,
                               AlarmEventType alarmEvent,
                               AlarmDeliveryResult result) {
        XxlJobAlarmRecord record = new XxlJobAlarmRecord();
        record.setJobGroup(info.getJobGroup());
        record.setJobId(info.getId());
        record.setJobLogId(jobLog.getId());
        record.setJobDesc(info.getJobDesc());
        record.setChannelId(channel != null ? channel.getId() : null);
        record.setChannelName(channel != null ? channel.getName() : "Legacy Email");
        record.setChannelType(channel != null ? channel.getType() : AlarmChannelType.EMAIL.name());
        record.setAlarmEvent(alarmEvent != null ? alarmEvent.name() : "");
        record.setTarget(target);
        record.setAlarmTitle(title);
        record.setAlarmContent(content);
        record.setSendStatus(result.isSuccess() ? 1 : 2);
        record.setResponseCode(result.getResponseCode());
        record.setResponseBody(truncate(result.getResponseBody(), 2000));
        record.setErrorMsg(truncate(result.getErrorMsg(), 500));
        record.setCreateTime(new Date());
        xxlJobAlarmRecordMapper.save(record);
    }

    private void persistRuleRecord(XxlJobInfo info,
                                   XxlJobLog jobLog,
                                   XxlJobAlarmRule rule,
                                   XxlJobAlarmChannel channel,
                                   AlarmEventType alarmEvent,
                                   AlarmDeliveryResult result) {
        String title = AlarmContentHelper.buildTitle(info);
        String content = AlarmContentHelper.buildHtmlContent(info, jobLog);
        String target = channel != null ? targetOf(channel) : rule.getChannelIds();
        persistRecord(info, jobLog, channel, target, title, content, alarmEvent, result);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private AlarmEventType currentEvent(XxlJobLog jobLog) {
        return AlarmEventType.of(jobLog.getTriggerCode(), jobLog.getHandleCode());
    }
}
