package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.core.context.XxlJobContext;

import java.text.MessageFormat;

public class AlarmContentHelper {

    private AlarmContentHelper() {
    }

    public static String buildTitle(XxlJobInfo info) {
        return I18nUtil.getString("jobconf_monitor") + " - " + info.getJobDesc() + " (#" + info.getId() + ")";
    }

    public static String buildSummary(XxlJobLog jobLog) {
        StringBuilder builder = new StringBuilder("Alarm Job LogId=").append(jobLog.getId());
        if (jobLog.getTriggerCode() != XxlJobContext.HANDLE_CODE_SUCCESS) {
            builder.append("<br>TriggerMsg=<br>").append(defaultText(jobLog.getTriggerMsg()));
        }
        if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != XxlJobContext.HANDLE_CODE_SUCCESS) {
            builder.append("<br>HandleMsg=<br>").append(defaultText(jobLog.getHandleMsg()));
        }
        return builder.toString();
    }

    public static String buildHtmlContent(XxlJobInfo info, XxlJobLog jobLog) {
        XxlJobGroup group = XxlJobAdminBootstrap.getInstance().getXxlJobGroupMapper().load(info.getJobGroup());
        return MessageFormat.format(loadHtmlTemplate(),
                group != null ? group.getTitle() : "null",
                info.getId(),
                info.getJobDesc(),
                buildSummary(jobLog));
    }

    public static String buildPlainTextContent(XxlJobInfo info, XxlJobLog jobLog) {
        XxlJobGroup group = XxlJobAdminBootstrap.getInstance().getXxlJobGroupMapper().load(info.getJobGroup());
        return "任务告警\n"
                + "执行器: " + (group != null ? group.getTitle() : "null") + "\n"
                + "任务ID: " + info.getId() + "\n"
                + "任务描述: " + info.getJobDesc() + "\n"
                + "日志ID: " + jobLog.getId() + "\n"
                + "触发结果: " + jobLog.getTriggerCode() + "\n"
                + "执行结果: " + jobLog.getHandleCode() + "\n"
                + "触发日志: " + defaultText(jobLog.getTriggerMsg()) + "\n"
                + "执行日志: " + defaultText(jobLog.getHandleMsg());
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }

    private static String loadHtmlTemplate() {
        return "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>"
                + "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n"
                + "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >"
                + "      <tr>\n"
                + "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobgroup") + "</td>\n"
                + "         <td width=\"10%\" >" + I18nUtil.getString("jobinfo_field_id") + "</td>\n"
                + "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobdesc") + "</td>\n"
                + "         <td width=\"10%\" >" + I18nUtil.getString("jobconf_monitor_alarm_title") + "</td>\n"
                + "         <td width=\"40%\" >" + I18nUtil.getString("jobconf_monitor_alarm_content") + "</td>\n"
                + "      </tr>\n"
                + "   </thead>\n"
                + "   <tbody>\n"
                + "      <tr>\n"
                + "         <td>{0}</td>\n"
                + "         <td>{1}</td>\n"
                + "         <td>{2}</td>\n"
                + "         <td>" + I18nUtil.getString("jobconf_monitor_alarm_type") + "</td>\n"
                + "         <td>{3}</td>\n"
                + "      </tr>\n"
                + "   </tbody>\n"
                + "</table>";
    }
}
