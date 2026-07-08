package com.xxl.job.core.openapi.client;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.http.HttpTool;

public class HttpExecutorBizClientTransport implements ExecutorBizClientTransport {

    @Override
    public String type() {
        return "HTTP";
    }

    @Override
    public boolean supports(String address) {
        if (StringTool.isBlank(address)) {
            return false;
        }
        String finalAddress = address.trim();
        if (!finalAddress.contains("://")) {
            return true;
        }
        return HttpTool.isHttp(finalAddress) || HttpTool.isHttps(finalAddress);
    }

    @Override
    public String normalizeAddress(String address) {
        String finalAddress = address.trim();
        if (!HttpTool.isHttp(finalAddress) && !HttpTool.isHttps(finalAddress)) {
            finalAddress = "http://" + StringTool.removePrefix(finalAddress, "//");
        }
        return finalAddress.endsWith("/") ? finalAddress : finalAddress + "/";
    }

    @Override
    public ExecutorBiz create(String address, String accessToken, int timeout) {
        int finalTimeout = (timeout >= 1 && timeout <= 10) ? timeout : 3;
        return new HttpExecutorBizClient(address, accessToken, finalTimeout);
    }
}
