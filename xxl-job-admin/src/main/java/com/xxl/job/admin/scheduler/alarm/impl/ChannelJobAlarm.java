package com.xxl.job.admin.scheduler.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.alarm.JobAlarm;
import com.xxl.tool.core.StringTool;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChannelJobAlarm implements JobAlarm {

    @Resource
    private AlarmChannelService alarmChannelService;

    @Value("${xxl.job.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        if (info == null) {
            return true;
        }
        boolean boundResult = true;
        if (StringTool.isNotBlank(info.getAlarmChannelIds())) {
            boundResult = alarmChannelService.sendBoundChannels(info, jobLog);
        } else if (!mailEnabled || StringTool.isBlank(info.getAlarmEmail())) {
            boundResult = alarmChannelService.sendExecutorDefaults(info, jobLog);
        }
        return boundResult;
    }
}
