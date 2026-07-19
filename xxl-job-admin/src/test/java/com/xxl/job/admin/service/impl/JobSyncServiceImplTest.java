package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.constant.TriggerStatus;
import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.core.constant.XxlJobStartPolicy;
import com.xxl.job.core.openapi.model.JobSyncItem;
import com.xxl.job.core.openapi.model.JobSyncRequest;
import com.xxl.tool.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JobSyncServiceImplTest {

    @Mock
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Mock
    private XxlJobInfoMapper xxlJobInfoMapper;
    @Mock
    private AlarmChannelService alarmChannelService;
    @Mock
    private AuditLogService auditLogService;

    private JobSyncServiceImpl jobSyncService;

    @BeforeEach
    void setUp() {
        I18nUtil i18nUtil = new I18nUtil();
        ReflectionTestUtils.setField(i18nUtil, "i18n", "zh_CN");
        ReflectionTestUtils.setField(I18nUtil.class, "single", i18nUtil);
        ReflectionTestUtils.setField(I18nUtil.class, "prop", null);

        jobSyncService = new JobSyncServiceImpl();
        ReflectionTestUtils.setField(jobSyncService, "xxlJobGroupMapper", xxlJobGroupMapper);
        ReflectionTestUtils.setField(jobSyncService, "xxlJobInfoMapper", xxlJobInfoMapper);
        ReflectionTestUtils.setField(jobSyncService, "alarmChannelService", alarmChannelService);
        ReflectionTestUtils.setField(jobSyncService, "auditLogService", auditLogService);
    }

    @Test
    void syncShouldPreserveStoppedExistingJobWhenStartPolicyIsOnCreate() {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("xxl-job-executor-sample");
        group.setTitle("sample");

        XxlJobInfo existing = buildExistingJob();
        JobSyncItem item = buildMatchingItem();
        item.setStartPolicy(XxlJobStartPolicy.ON_CREATE.name());

        when(xxlJobGroupMapper.loadByAppname(group.getAppname())).thenReturn(group);
        when(xxlJobInfoMapper.loadByGroupAndHandler(group.getId(), item.getExecutorHandler())).thenReturn(existing);
        when(alarmChannelService.normalizeChannelIdsToString(anyString())).thenReturn("");
        when(alarmChannelService.normalizeEventTypesToString(anyString())).thenReturn("");

        Response<String> response = jobSyncService.sync(buildRequest(group.getAppname(), item));

        assertTrue(response.isSuccess());
        verify(xxlJobInfoMapper, never()).update(any());
        assertEquals(TriggerStatus.STOPPED.getValue(), existing.getTriggerStatus());
        assertEquals(0, existing.getTriggerNextTime());
    }

    @Test
    void syncShouldStartNewJobWhenStartPolicyIsOnCreate() {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("xxl-job-executor-sample");
        group.setTitle("sample");

        JobSyncItem item = buildMatchingItem();
        item.setStartPolicy(XxlJobStartPolicy.ON_CREATE.name());

        when(xxlJobGroupMapper.loadByAppname(group.getAppname())).thenReturn(group);
        when(xxlJobInfoMapper.loadByGroupAndHandler(group.getId(), item.getExecutorHandler())).thenReturn(null);
        when(alarmChannelService.normalizeChannelIdsToString(anyString())).thenReturn("");
        when(alarmChannelService.normalizeEventTypesToString(anyString())).thenReturn("");

        Response<String> response = jobSyncService.sync(buildRequest(group.getAppname(), item));

        assertTrue(response.isSuccess());
        ArgumentCaptor<XxlJobInfo> captor = ArgumentCaptor.forClass(XxlJobInfo.class);
        verify(xxlJobInfoMapper).save(captor.capture());
        verify(xxlJobInfoMapper).update(captor.capture());
        XxlJobInfo started = captor.getAllValues().get(1);
        assertEquals(TriggerStatus.RUNNING.getValue(), started.getTriggerStatus());
        assertTrue(started.getTriggerNextTime() > System.currentTimeMillis());
    }

    @Test
    void syncShouldStartStoppedExistingJobWhenStartPolicyEnsuresRunning() {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("xxl-job-executor-sample");
        group.setTitle("sample");

        XxlJobInfo existing = buildExistingJob();
        JobSyncItem item = buildMatchingItem();
        item.setStartPolicy(XxlJobStartPolicy.ENSURE_RUNNING.name());

        when(xxlJobGroupMapper.loadByAppname(group.getAppname())).thenReturn(group);
        when(xxlJobInfoMapper.loadByGroupAndHandler(group.getId(), item.getExecutorHandler())).thenReturn(existing);
        when(alarmChannelService.normalizeChannelIdsToString(anyString())).thenReturn("");
        when(alarmChannelService.normalizeEventTypesToString(anyString())).thenReturn("");

        Response<String> response = jobSyncService.sync(buildRequest(group.getAppname(), item));

        assertTrue(response.isSuccess());
        ArgumentCaptor<XxlJobInfo> captor = ArgumentCaptor.forClass(XxlJobInfo.class);
        verify(xxlJobInfoMapper).update(captor.capture());
        XxlJobInfo updated = captor.getValue();
        assertEquals(TriggerStatus.RUNNING.getValue(), updated.getTriggerStatus());
        assertEquals(0, updated.getTriggerLastTime());
        assertTrue(updated.getTriggerNextTime() > System.currentTimeMillis());
    }

    @Test
    void syncShouldRefreshNextTriggerTimeWhenRunningJobScheduleChanged() {
        XxlJobGroup group = new XxlJobGroup();
        group.setId(1);
        group.setAppname("xxl-job-executor-sample");
        group.setTitle("sample");

        XxlJobInfo existing = buildExistingJob();
        existing.setTriggerStatus(TriggerStatus.RUNNING.getValue());
        existing.setTriggerLastTime(456L);
        existing.setTriggerNextTime(123L);

        JobSyncItem item = buildMatchingItem();
        item.setScheduleConf("*/10 * * * * ?");
        item.setStartPolicy(XxlJobStartPolicy.ON_CREATE.name());

        when(xxlJobGroupMapper.loadByAppname(group.getAppname())).thenReturn(group);
        when(xxlJobInfoMapper.loadByGroupAndHandler(group.getId(), item.getExecutorHandler())).thenReturn(existing);
        when(alarmChannelService.normalizeChannelIdsToString(any())).thenReturn("");
        when(alarmChannelService.normalizeEventTypesToString(any())).thenReturn("");

        Response<String> response = jobSyncService.sync(buildRequest(group.getAppname(), item));

        assertTrue(response.isSuccess());
        ArgumentCaptor<XxlJobInfo> captor = ArgumentCaptor.forClass(XxlJobInfo.class);
        verify(xxlJobInfoMapper).update(captor.capture());
        XxlJobInfo updated = captor.getValue();
        assertEquals(TriggerStatus.RUNNING.getValue(), updated.getTriggerStatus());
        assertEquals(456L, updated.getTriggerLastTime());
        assertTrue(updated.getTriggerNextTime() > System.currentTimeMillis());
    }

    private JobSyncRequest buildRequest(String appname, JobSyncItem item) {
        JobSyncRequest request = new JobSyncRequest();
        request.setAppname(appname);
        request.setGroupTitle("sample");
        request.setAddressType(0);
        request.setSyncMode("CREATE_UPDATE");
        request.setJobs(List.of(item));
        return request;
    }

    private XxlJobInfo buildExistingJob() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(100);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("demo job");
        jobInfo.setAuthor("XXL");
        jobInfo.setJobTag("sample,demo");
        jobInfo.setAlarmEmail("");
        jobInfo.setAlarmChannelIds("");
        jobInfo.setAlarmEventTypes("");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 0 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorParam("");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setExecutorTimeout(0);
        jobInfo.setExecutorFailRetryCount(0);
        jobInfo.setTriggerStatus(TriggerStatus.STOPPED.getValue());
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(0);
        return jobInfo;
    }

    private JobSyncItem buildMatchingItem() {
        JobSyncItem item = new JobSyncItem();
        item.setExecutorHandler("demoJobHandler");
        item.setJobDesc("demo job");
        item.setAuthor("XXL");
        item.setJobTag("sample,demo");
        item.setAlarmEmail("");
        item.setAlarmChannelIds("");
        item.setAlarmEventTypes("");
        item.setScheduleType("CRON");
        item.setScheduleConf("0 0 0 * * ? *");
        item.setMisfireStrategy("DO_NOTHING");
        item.setExecutorRouteStrategy("FIRST");
        item.setExecutorParam("");
        item.setExecutorBlockStrategy("SERIAL_EXECUTION");
        item.setExecutorTimeout(0);
        item.setExecutorFailRetryCount(0);
        return item;
    }
}
