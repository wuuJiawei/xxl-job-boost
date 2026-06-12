package com.xxl.job.core.openapi;

import com.xxl.job.core.constant.Const;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.http.HttpTool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExecutorBizClientFactory {

    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_REPOSITORY = new ConcurrentHashMap<String, ExecutorBiz>();

    public static ExecutorBiz getExecutorBiz(String address, String accessToken, int timeout) {
        if (StringTool.isBlank(address)) {
            return null;
        }

        String finalAddress = address.trim();
        ExecutorBiz executorBiz = EXECUTOR_BIZ_REPOSITORY.get(finalAddress);
        if (executorBiz != null) {
            return executorBiz;
        }

        int finalTimeout = (timeout >= 1 && timeout <= 10) ? timeout : 3;
        executorBiz = HttpTool.createClient()
                .url(finalAddress)
                .timeout(finalTimeout * 1000)
                .header(Const.XXL_JOB_ACCESS_TOKEN, accessToken)
                .proxy(ExecutorBiz.class);
        EXECUTOR_BIZ_REPOSITORY.put(finalAddress, executorBiz);
        return executorBiz;
    }

    private ExecutorBizClientFactory() {
    }
}
