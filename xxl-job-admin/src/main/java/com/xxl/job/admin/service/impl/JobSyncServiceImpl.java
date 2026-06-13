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

@Service
public class JobSyncServiceImpl implements JobSyncService {
    private static final Logger logger = LoggerFactory.getLogger(JobSyncServiceImpl.class);

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    private AlarmChannelService alarmChannelService;

    @Override
    public Response<String> sync(JobSyncRequest request) {
        if (request == null || StringTool.isBlank(request.getAppname())) {
            return Response.ofFail("appname不能为空");
        }

        JobSyncMode syncMode = JobSyncMode.match(request.getSyncMode());
        if (syncMode == JobSyncMode.DISABLED) {
            return Response.ofSuccess("sync disabled");
        }

        XxlJobGroup group = ensureGroup(request);
        if (request.getJobs() == null || request.getJobs().isEmpty()) {
            return Response.ofSuccess("group synced");
        }

        for (JobSyncItem item : request.getJobs()) {
            syncJob(group, item, syncMode);
        }
        return Response.ofSuccess(String.format("group=%s, jobs=%d, mode=%s", group.getAppname(), request.getJobs().size(), syncMode.name()));
    }

    private XxlJobGroup ensureGroup(JobSyncRequest request) {
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
        }
        return group;
    }

    private void syncJob(XxlJobGroup group, JobSyncItem item, JobSyncMode syncMode) {
        if (item == null || StringTool.isBlank(item.getExecutorHandler()) || StringTool.isBlank(item.getJobDesc())) {
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
            logger.info(">>>>>>>>>>> xxl-job boost sync create job success, appname:{}, handler:{}, jobId:{}",
                    group.getAppname(), item.getExecutorHandler(), jobInfo.getId());
            return;
        }

        if (syncMode != JobSyncMode.CREATE_UPDATE) {
            return;
        }

        fillJob(exists, group.getId(), item);
        exists.setUpdateTime(new Date());
        xxlJobInfoMapper.update(exists);
        if (item.isAutoStart() && exists.getTriggerStatus() != TriggerStatus.RUNNING.getValue()) {
            startJob(exists);
        }
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
}
