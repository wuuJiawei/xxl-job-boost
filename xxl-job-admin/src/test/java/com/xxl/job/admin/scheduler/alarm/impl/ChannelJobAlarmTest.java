package com.xxl.job.admin.scheduler.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelJobAlarmTest {

    @Mock
    private AlarmChannelService alarmChannelService;

    private ChannelJobAlarm channelJobAlarm;
    private XxlJobLog jobLog;

    @BeforeEach
    void setUp() {
        channelJobAlarm = new ChannelJobAlarm();
        ReflectionTestUtils.setField(channelJobAlarm, "alarmChannelService", alarmChannelService);
        jobLog = new XxlJobLog();
    }

    @Test
    void taskChannelsShouldOverrideExecutorDefaults() {
        XxlJobInfo info = jobInfo("1,2", "");
        when(alarmChannelService.sendBoundChannels(info, jobLog)).thenReturn(true);

        assertTrue(channelJobAlarm.doAlarm(info, jobLog));

        verify(alarmChannelService).sendBoundChannels(info, jobLog);
        verify(alarmChannelService, never()).sendExecutorDefaults(info, jobLog);
    }

    @Test
    void enabledLegacyTaskEmailShouldOverrideExecutorDefaults() {
        XxlJobInfo info = jobInfo("", "ops@example.com");
        ReflectionTestUtils.setField(channelJobAlarm, "mailEnabled", true);

        assertTrue(channelJobAlarm.doAlarm(info, jobLog));

        verify(alarmChannelService, never()).sendBoundChannels(info, jobLog);
        verify(alarmChannelService, never()).sendExecutorDefaults(info, jobLog);
    }

    @Test
    void disabledLegacyTaskEmailShouldInheritExecutorDefaults() {
        XxlJobInfo info = jobInfo("", "ops@example.com");
        when(alarmChannelService.sendExecutorDefaults(info, jobLog)).thenReturn(true);

        assertTrue(channelJobAlarm.doAlarm(info, jobLog));

        verify(alarmChannelService, never()).sendBoundChannels(info, jobLog);
        verify(alarmChannelService).sendExecutorDefaults(info, jobLog);
    }

    @Test
    void taskWithoutAlarmConfigShouldInheritExecutorDefaults() {
        XxlJobInfo info = jobInfo("", "");
        when(alarmChannelService.sendExecutorDefaults(info, jobLog)).thenReturn(true);

        assertTrue(channelJobAlarm.doAlarm(info, jobLog));

        verify(alarmChannelService, never()).sendBoundChannels(info, jobLog);
        verify(alarmChannelService).sendExecutorDefaults(info, jobLog);
    }

    private XxlJobInfo jobInfo(String channelIds, String alarmEmail) {
        XxlJobInfo info = new XxlJobInfo();
        info.setAlarmChannelIds(channelIds);
        info.setAlarmEmail(alarmEmail);
        return info;
    }
}
