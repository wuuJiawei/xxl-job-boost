package com.xxl.job.core.openapi.client;

import com.xxl.tool.core.StringTool;

import java.util.ArrayList;
import java.util.List;

public class ExecutorBizEndpointListHelper {

    public static List<String> split(String addressList) {
        List<String> result = new ArrayList<String>();
        if (StringTool.isBlank(addressList)) {
            return result;
        }
        for (String item : addressList.split(",")) {
            if (StringTool.isBlank(item)) {
                continue;
            }
            result.add(item.trim());
        }
        return result;
    }

    public static String join(List<String> addressList) {
        if (addressList == null || addressList.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<String>();
        for (String item : addressList) {
            if (StringTool.isBlank(item)) {
                continue;
            }
            values.add(item.trim());
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    public static List<String> normalize(List<String> addressList) {
        List<String> normalized = new ArrayList<String>();
        if (addressList == null) {
            return normalized;
        }
        for (String endpoint : addressList) {
            if (StringTool.isBlank(endpoint)) {
                continue;
            }
            normalized.add(ExecutorBizClientTransportFactory.normalizeEndpoint(endpoint));
        }
        return normalized;
    }

    public static String normalizeAndJoin(String addressList) {
        return join(normalize(split(addressList)));
    }

    private ExecutorBizEndpointListHelper() {
    }
}
