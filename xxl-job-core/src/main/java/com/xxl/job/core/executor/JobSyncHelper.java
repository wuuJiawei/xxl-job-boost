package com.xxl.job.core.executor;

import com.xxl.job.core.constant.Const;
import com.xxl.job.core.handler.annotation.XxlJobBoost;
import com.xxl.job.core.constant.XxlJobAlarmEventType;
import com.xxl.job.core.openapi.model.JobSyncItem;
import com.xxl.job.core.openapi.model.JobSyncRequest;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.http.HttpTool;
import com.xxl.tool.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JobSyncHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobSyncHelper.class);

    public static void sync(XxlJobExecutor executor, List<JobSyncItem> jobSyncItems) {
        if (executor == null || jobSyncItems == null || jobSyncItems.isEmpty()) {
            return;
        }
        if (StringTool.isBlank(executor.getAdminAddresses())
                || StringTool.isBlank(executor.getAppname())
                || StringTool.isBlank(executor.getSyncMode())
                || "DISABLED".equalsIgnoreCase(executor.getSyncMode())) {
            return;
        }

        JobSyncRequest request = new JobSyncRequest();
        request.setAppname(executor.getAppname());
        request.setGroupTitle(StringTool.isBlank(executor.getGroupTitle()) ? executor.getAppname() : executor.getGroupTitle());
        request.setAddressType(0);
        request.setSyncMode(executor.getSyncMode());
        request.setJobs(jobSyncItems);

        for (String address : executor.getAdminAddresses().trim().split(",")) {
            String finalAddress = address == null ? "" : address.trim();
            if (finalAddress.isEmpty()) {
                continue;
            }
            finalAddress = finalAddress.endsWith("/") ? (finalAddress + "api") : (finalAddress + "/api");
            try {
                Response response = HttpTool.createClient()
                        .url(finalAddress)
                        .timeout(Math.max(executor.getTimeout(), 3) * 1000)
                        .header(Const.XXL_JOB_ACCESS_TOKEN, executor.getAccessToken())
                        .proxy(com.xxl.job.core.openapi.AdminBiz.class)
                        .syncJobs(request);
                if (response != null && response.isSuccess()) {
                    logger.info(">>>>>>>>>>> xxl-job boost sync success, appname:{}, jobs:{}, summary:{}",
                            executor.getAppname(), jobSyncItems.size(), response.getData());
                    return;
                }
                logger.warn(">>>>>>>>>>> xxl-job boost sync fail, appname:{}, response:{}", executor.getAppname(), response);
            } catch (Exception e) {
                logger.warn(">>>>>>>>>>> xxl-job boost sync error, appname:{}, apiBase:{}, message:{}",
                        executor.getAppname(), finalAddress, e.getMessage());
            }
        }
    }

    public static JobSyncItem toItem(String handlerName, XxlJobBoost xxlJobBoost, Method method) {
        if (xxlJobBoost == null || StringTool.isBlank(handlerName)) {
            return null;
        }
        JobSyncItem item = new JobSyncItem();
        item.setExecutorHandler(handlerName.trim());
        item.setJobDesc(xxlJobBoost.desc().trim());
        item.setAuthor(xxlJobBoost.author().trim());
        item.setJobTag(xxlJobBoost.jobTag().trim());
        item.setAlarmEmail(xxlJobBoost.alarmEmail().trim());
        item.setAlarmChannelIds(xxlJobBoost.alarmChannelIds().trim());
        item.setAlarmEventTypes(toAlarmEventTypes(xxlJobBoost.alarmEventTypes()));
        item.setScheduleType(xxlJobBoost.scheduleType().name());
        item.setScheduleConf(xxlJobBoost.scheduleConf().trim());
        item.setExecutorParam(xxlJobBoost.executorParam());
        item.setExecutorRouteStrategy(xxlJobBoost.routeStrategy().name());
        item.setMisfireStrategy(xxlJobBoost.misfireStrategy().name());
        item.setExecutorBlockStrategy(xxlJobBoost.blockStrategy().name());
        item.setExecutorTimeout(xxlJobBoost.timeout());
        item.setExecutorFailRetryCount(xxlJobBoost.retryCount());
        item.setAutoStart(xxlJobBoost.autoStart());
        return item;
    }

    private static String toAlarmEventTypes(XxlJobAlarmEventType[] alarmEventTypes) {
        if (alarmEventTypes == null || alarmEventTypes.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (XxlJobAlarmEventType alarmEventType : alarmEventTypes) {
            if (alarmEventType == null) {
                continue;
            }
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(alarmEventType.name());
        }
        return result.toString();
    }

    public static List<JobSyncItem> newItems() {
        return new ArrayList<>();
    }
}
