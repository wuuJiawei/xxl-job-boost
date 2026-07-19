package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.mapper.XxlJobAlarmChannelMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRecordMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRuleMapper;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.model.XxlJobAlarmChannel;
import com.xxl.job.admin.model.XxlJobAlarmRule;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.admin.util.I18nUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmChannelServiceTest {

    @Mock
    private XxlJobAlarmChannelMapper channelMapper;
    @Mock
    private XxlJobAlarmRecordMapper recordMapper;
    @Mock
    private XxlJobAlarmRuleMapper ruleMapper;
    @Mock
    private XxlJobGroupMapper groupMapper;

    private AlarmChannelService alarmChannelService;

    @BeforeEach
    void setUp() {
        I18nUtil i18nUtil = new I18nUtil();
        ReflectionTestUtils.setField(i18nUtil, "i18n", "zh_CN");
        ReflectionTestUtils.setField(I18nUtil.class, "single", i18nUtil);
        ReflectionTestUtils.setField(I18nUtil.class, "prop", null);

        XxlJobAdminBootstrap bootstrap = new XxlJobAdminBootstrap();
        ReflectionTestUtils.setField(bootstrap, "xxlJobGroupMapper", groupMapper);
        ReflectionTestUtils.setField(XxlJobAdminBootstrap.class, "adminConfig", bootstrap);

        alarmChannelService = new AlarmChannelService();
        ReflectionTestUtils.setField(alarmChannelService, "xxlJobAlarmChannelMapper", channelMapper);
        ReflectionTestUtils.setField(alarmChannelService, "xxlJobAlarmRecordMapper", recordMapper);
        ReflectionTestUtils.setField(alarmChannelService, "xxlJobAlarmRuleMapper", ruleMapper);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(XxlJobAdminBootstrap.class, "adminConfig", null);
    }

    @Test
    void executorDefaultsShouldDeduplicateChannelsAcrossHistoricalRules() {
        XxlJobAlarmRule first = rule("1,2");
        XxlJobAlarmRule second = rule("2");
        when(ruleMapper.findEnabledExecutorDefaults(1, AlarmEventType.EXECUTOR_FAIL.name()))
                .thenReturn(List.of(first, second));

        XxlJobAlarmChannel channelOne = disabledChannel(1);
        XxlJobAlarmChannel channelTwo = disabledChannel(2);
        when(channelMapper.findByIds(List.of(1, 2))).thenReturn(List.of(channelOne, channelTwo));

        XxlJobInfo info = new XxlJobInfo();
        info.setId(10);
        info.setJobGroup(1);
        info.setJobDesc("demo");
        XxlJobLog jobLog = new XxlJobLog();
        jobLog.setTriggerCode(200);
        jobLog.setHandleCode(500);

        assertFalse(alarmChannelService.sendExecutorDefaults(info, jobLog));

        verify(channelMapper).findByIds(List.of(1, 2));
        verify(recordMapper, times(2)).save(any());
    }

    private XxlJobAlarmRule rule(String channelIds) {
        XxlJobAlarmRule rule = new XxlJobAlarmRule();
        rule.setChannelIds(channelIds);
        return rule;
    }

    private XxlJobAlarmChannel disabledChannel(int id) {
        XxlJobAlarmChannel channel = new XxlJobAlarmChannel();
        channel.setId(id);
        channel.setName("channel-" + id);
        channel.setType(AlarmChannelType.WEBHOOK.name());
        channel.setEnabled(0);
        return channel;
    }
}
