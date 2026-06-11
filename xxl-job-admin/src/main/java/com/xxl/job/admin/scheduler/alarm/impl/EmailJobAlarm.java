package com.xxl.job.admin.scheduler.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.core.alarm.AlarmContentHelper;
import com.xxl.job.admin.core.alarm.AlarmDeliveryResult;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.alarm.JobAlarm;
import com.xxl.job.admin.scheduler.config.XxlJobAdminBootstrap;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * job alarm by email
 *
 * @author xuxueli 2020-01-19
 */
@Component
public class EmailJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    @Resource
    private AlarmChannelService alarmChannelService;

    /**
     * fail alarm
     *
     * @param jobLog
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog){
        boolean alarmResult = true;

        // send monitor email
        if (info!=null && info.getAlarmEmail()!=null && !info.getAlarmEmail().trim().isEmpty()) {
            String personal = com.xxl.job.admin.util.I18nUtil.getString("admin_name_full");
            String title = AlarmContentHelper.buildTitle(info);
            String content = AlarmContentHelper.buildHtmlContent(info, jobLog);

            Set<String> emailSet = new HashSet<String>(Arrays.asList(info.getAlarmEmail().split(",")));
            for (String email: emailSet) {
                String target = email == null ? "" : email.trim();
                if (target.isEmpty()) {
                    continue;
                }

                // make mail
                try {
                    MimeMessage mimeMessage = XxlJobAdminBootstrap.getInstance().getMailSender().createMimeMessage();

                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(XxlJobAdminBootstrap.getInstance().getEmailFrom(), personal);
                    helper.setTo(target);
                    helper.setSubject(title);
                    helper.setText(content, true);

                    XxlJobAdminBootstrap.getInstance().getMailSender().send(mimeMessage);
                    alarmChannelService.recordLegacyEmail(info, jobLog, target, title, content,
                            AlarmDeliveryResult.success(200, "OK"));
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);
                    alarmChannelService.recordLegacyEmail(info, jobLog, target, title, content,
                            AlarmDeliveryResult.fail(null, null, e.getMessage()));
                    alarmResult = false;
                }

            }
        }

        return alarmResult;
    }
}
