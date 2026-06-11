package com.xxl.job.admin.scheduler.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.alarm.JobAlarm;
import com.xxl.tool.core.StringTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ChannelJobAlarm implements JobAlarm {

    @Resource
    private AlarmChannelService alarmChannelService;

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        if (info == null || StringTool.isBlank(info.getAlarmChannelIds())) {
            return true;
        }
        return alarmChannelService.sendBoundChannels(info, jobLog);
    }
}
