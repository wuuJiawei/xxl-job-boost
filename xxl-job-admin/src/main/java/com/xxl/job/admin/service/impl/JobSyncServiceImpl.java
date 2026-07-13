package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.constant.TriggerStatus;
import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.core.sync.JobSyncMode;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.scheduler.thread.JobScheduleHelper;
import com.xxl.job.admin.scheduler.type.ScheduleTypeEnum;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.job.admin.service.JobSyncService;
import com.xxl.job.core.constant.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.openapi.model.JobSyncItem;
import com.xxl.job.core.openapi.model.JobSyncRequest;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JobSyncServiceImpl implements JobSyncService {
    private static final Logger logger = LoggerFactory.getLogger(JobSyncServiceImpl.class);
    private static final String SYNC_OPERATOR_USER_ID = "system:job-sync";
    private static final String SYNC_OPERATOR_NAME = "job-sync";
    private static final String SYNC_SOURCE = "executor-sync";

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    private AlarmChannelService alarmChannelService;
    @Resource
    private AuditLogService auditLogService;

    @Override
    public Response<String> sync(JobSyncRequest request) {
        if (request == null || StringTool.isBlank(request.getAppname())) {
            return Response.ofFail("appname不能为空");
        }

        JobSyncMode syncMode = JobSyncMode.match(request.getSyncMode());
        if (syncMode == JobSyncMode.DISABLED) {
            return Response.ofSuccess("sync disabled");
        }

        SyncSummary summary = new SyncSummary();
        summary.mode = syncMode.name();
        summary.appname = request.getAppname().trim();
        XxlJobGroup group = ensureGroup(request, summary);
        summary.jobGroup = group.getId();
        if (request.getJobs() == null || request.getJobs().isEmpty()) {
            return Response.ofSuccess(summary.toMessage());
        }

        summary.requested = request.getJobs().size();
        for (JobSyncItem item : request.getJobs()) {
            syncJob(group, item, syncMode, summary);
        }
        return Response.ofSuccess(summary.toMessage());
    }

    private XxlJobGroup ensureGroup(JobSyncRequest request, SyncSummary summary) {
        XxlJobGroup group = xxlJobGroupMapper.loadByAppname(request.getAppname());
        Date now = new Date();
        if (group == null) {
            group = new XxlJobGroup();
            group.setAppname(request.getAppname());
            group.setTitle(StringTool.isBlank(request.getGroupTitle()) ? request.getAppname() : request.getGroupTitle().trim());
            group.setAddressType(request.getAddressType());
            group.setAddressList(null);
            group.setUpdateTime(now);
            xxlJobGroupMapper.save(group);
            summary.groupCreated = true;
            logger.info(">>>>>>>>>>> xxl-job boost sync create group success, appname:{}, groupId:{}", group.getAppname(), group.getId());
            return group;
        }

        boolean changed = false;
        String nextTitle = StringTool.isBlank(request.getGroupTitle()) ? group.getTitle() : request.getGroupTitle().trim();
        if (!nextTitle.equals(group.getTitle())) {
            group.setTitle(nextTitle);
            changed = true;
        }
        if (request.getAddressType() != group.getAddressType()) {
            group.setAddressType(request.getAddressType());
            changed = true;
        }
        if (changed) {
            group.setUpdateTime(now);
            xxlJobGroupMapper.update(group);
            summary.groupUpdated = true;
        }
        return group;
    }

    private void syncJob(XxlJobGroup group, JobSyncItem item, JobSyncMode syncMode, SyncSummary summary) {
        if (item == null || StringTool.isBlank(item.getExecutorHandler()) || StringTool.isBlank(item.getJobDesc())) {
            summary.skipped++;
            return;
        }

        XxlJobInfo exists = xxlJobInfoMapper.loadByGroupAndHandler(group.getId(), item.getExecutorHandler().trim());
        if (exists == null) {
            XxlJobInfo jobInfo = new XxlJobInfo();
            fillJob(jobInfo, group.getId(), item);
            Date now = new Date();
            jobInfo.setAddTime(now);
            jobInfo.setUpdateTime(now);
            jobInfo.setGlueUpdatetime(now);
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
            jobInfo.setGlueSource("");
            jobInfo.setGlueRemark("XxlJobBoost sync init");
            jobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(0);
            xxlJobInfoMapper.save(jobInfo);
            if (item.isAutoStart()) {
                startJob(jobInfo);
            }
            summary.created++;
            recordSyncAudit("job-sync-create", jobInfo, item, buildCreateDetail(group, item, jobInfo));
            logger.info(">>>>>>>>>>> xxl-job boost sync create job success, appname:{}, handler:{}, jobId:{}",
                    group.getAppname(), item.getExecutorHandler(), jobInfo.getId());
            return;
        }

        if (syncMode != JobSyncMode.CREATE_UPDATE) {
            summary.unchanged++;
            return;
        }

        Map<String, Object> diff = buildDiff(exists, item);
        if (diff.isEmpty()) {
            if (item.isAutoStart()) {
                startJob(exists);
            }
            summary.unchanged++;
            return;
        }

        fillJob(exists, group.getId(), item);
        exists.setUpdateTime(new Date());
        if (item.isAutoStart()) {
            startJob(exists);
        } else {
            xxlJobInfoMapper.update(exists);
        }
        summary.updated++;
        recordSyncAudit("job-sync-update", exists, item, buildUpdateDetail(group, item, exists, diff));
        logger.info(">>>>>>>>>>> xxl-job boost sync update job success, appname:{}, handler:{}, jobId:{}",
                group.getAppname(), item.getExecutorHandler(), exists.getId());
    }

    private void fillJob(XxlJobInfo jobInfo, int jobGroupId, JobSyncItem item) {
        jobInfo.setJobGroup(jobGroupId);
        jobInfo.setJobDesc(item.getJobDesc().trim());
        jobInfo.setAuthor(item.getAuthor().trim());
        jobInfo.setJobTag(normalizeText(item.getJobTag()));
        jobInfo.setAlarmEmail(normalizeText(item.getAlarmEmail()));
        jobInfo.setAlarmChannelIds(alarmChannelService.normalizeChannelIdsToString(item.getAlarmChannelIds()));
        jobInfo.setAlarmEventTypes(alarmChannelService.normalizeEventTypesToString(item.getAlarmEventTypes()));
        jobInfo.setScheduleType(item.getScheduleType());
        jobInfo.setScheduleConf(item.getScheduleConf());
        jobInfo.setMisfireStrategy(item.getMisfireStrategy());
        jobInfo.setExecutorRouteStrategy(item.getExecutorRouteStrategy());
        jobInfo.setExecutorHandler(item.getExecutorHandler().trim());
        jobInfo.setExecutorParam(item.getExecutorParam());
        jobInfo.setExecutorBlockStrategy(item.getExecutorBlockStrategy());
        jobInfo.setExecutorTimeout(item.getExecutorTimeout());
        jobInfo.setExecutorFailRetryCount(item.getExecutorFailRetryCount());
        jobInfo.setChildJobId("");
        if (jobInfo.getGlueType() == null) {
            jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
        }
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<String, Object> buildDiff(XxlJobInfo exists, JobSyncItem item) {
        Map<String, Object> diff = new LinkedHashMap<>();
        putDiff(diff, "jobDesc", exists.getJobDesc(), item.getJobDesc().trim());
        putDiff(diff, "author", exists.getAuthor(), item.getAuthor().trim());
        putDiff(diff, "jobTag", exists.getJobTag(), normalizeText(item.getJobTag()));
        putDiff(diff, "alarmEmail", exists.getAlarmEmail(), normalizeText(item.getAlarmEmail()));
        putDiff(diff, "alarmChannelIds", exists.getAlarmChannelIds(), alarmChannelService.normalizeChannelIdsToString(item.getAlarmChannelIds()));
        putDiff(diff, "alarmEventTypes", exists.getAlarmEventTypes(), alarmChannelService.normalizeEventTypesToString(item.getAlarmEventTypes()));
        putDiff(diff, "scheduleType", exists.getScheduleType(), item.getScheduleType());
        putDiff(diff, "scheduleConf", exists.getScheduleConf(), item.getScheduleConf());
        putDiff(diff, "misfireStrategy", exists.getMisfireStrategy(), item.getMisfireStrategy());
        putDiff(diff, "executorRouteStrategy", exists.getExecutorRouteStrategy(), item.getExecutorRouteStrategy());
        putDiff(diff, "executorHandler", exists.getExecutorHandler(), item.getExecutorHandler().trim());
        putDiff(diff, "executorParam", exists.getExecutorParam(), item.getExecutorParam());
        putDiff(diff, "executorBlockStrategy", exists.getExecutorBlockStrategy(), item.getExecutorBlockStrategy());
        putDiff(diff, "executorTimeout", exists.getExecutorTimeout(), item.getExecutorTimeout());
        putDiff(diff, "executorFailRetryCount", exists.getExecutorFailRetryCount(), item.getExecutorFailRetryCount());
        return diff;
    }

    private void putDiff(Map<String, Object> diff, String field, Object before, Object after) {
        if (sameValue(before, after)) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("before", before);
        item.put("after", after);
        diff.put(field, item);
    }

    private boolean sameValue(Object before, Object after) {
        if (before == null) {
            return after == null;
        }
        return before.equals(after);
    }

    private Map<String, Object> buildCreateDetail(XxlJobGroup group, JobSyncItem item, XxlJobInfo jobInfo) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("appname", group.getAppname());
        detail.put("groupId", group.getId());
        detail.put("handler", item.getExecutorHandler().trim());
        detail.put("jobId", jobInfo.getId());
        detail.put("mode", "CREATE");
        detail.put("autoStart", item.isAutoStart());
        detail.put("snapshot", snapshot(jobInfo));
        return detail;
    }

    private Map<String, Object> buildUpdateDetail(XxlJobGroup group, JobSyncItem item, XxlJobInfo jobInfo, Map<String, Object> diff) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("appname", group.getAppname());
        detail.put("groupId", group.getId());
        detail.put("handler", item.getExecutorHandler().trim());
        detail.put("jobId", jobInfo.getId());
        detail.put("mode", "UPDATE");
        detail.put("autoStart", item.isAutoStart());
        detail.put("diff", diff);
        return detail;
    }

    private Map<String, Object> snapshot(XxlJobInfo jobInfo) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("jobDesc", jobInfo.getJobDesc());
        snapshot.put("author", jobInfo.getAuthor());
        snapshot.put("jobTag", jobInfo.getJobTag());
        snapshot.put("alarmEmail", jobInfo.getAlarmEmail());
        snapshot.put("alarmChannelIds", jobInfo.getAlarmChannelIds());
        snapshot.put("alarmEventTypes", jobInfo.getAlarmEventTypes());
        snapshot.put("scheduleType", jobInfo.getScheduleType());
        snapshot.put("scheduleConf", jobInfo.getScheduleConf());
        snapshot.put("misfireStrategy", jobInfo.getMisfireStrategy());
        snapshot.put("executorRouteStrategy", jobInfo.getExecutorRouteStrategy());
        snapshot.put("executorHandler", jobInfo.getExecutorHandler());
        snapshot.put("executorParam", jobInfo.getExecutorParam());
        snapshot.put("executorBlockStrategy", jobInfo.getExecutorBlockStrategy());
        snapshot.put("executorTimeout", jobInfo.getExecutorTimeout());
        snapshot.put("executorFailRetryCount", jobInfo.getExecutorFailRetryCount());
        snapshot.put("triggerStatus", jobInfo.getTriggerStatus());
        return snapshot;
    }

    private void recordSyncAudit(String actionType, XxlJobInfo jobInfo, JobSyncItem item, Map<String, Object> detail) {
        auditLogService.recordSystem(
                SYNC_OPERATOR_USER_ID,
                SYNC_OPERATOR_NAME,
                SYNC_SOURCE,
                actionType,
                "job",
                String.valueOf(jobInfo.getId()),
                item.getJobDesc().trim(),
                jobInfo.getJobGroup(),
                detail
        );
    }

    private void startJob(XxlJobInfo jobInfo) {
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            return;
        }
        try {
            Date nextValidTime = scheduleTypeEnum.getScheduleType()
                    .generateNextTriggerTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return;
            }
            jobInfo.setTriggerStatus(TriggerStatus.RUNNING.getValue());
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
            jobInfo.setUpdateTime(new Date());
            xxlJobInfoMapper.update(jobInfo);
        } catch (Exception e) {
            logger.warn(">>>>>>>>>>> xxl-job boost sync start job skipped, handler:{}, message:{}",
                    jobInfo.getExecutorHandler(), e.getMessage());
        }
    }

    private static class SyncSummary {
        private String appname;
        private String mode;
        private int jobGroup;
        private int requested;
        private int created;
        private int updated;
        private int unchanged;
        private int skipped;
        private boolean groupCreated;
        private boolean groupUpdated;

        private String toMessage() {
            return String.format(
                    "appname=%s, groupId=%d, mode=%s, requested=%d, created=%d, updated=%d, unchanged=%d, skipped=%d, groupCreated=%s, groupUpdated=%s",
                    appname, jobGroup, mode, requested, created, updated, unchanged, skipped, groupCreated, groupUpdated
            );
        }
    }
}
