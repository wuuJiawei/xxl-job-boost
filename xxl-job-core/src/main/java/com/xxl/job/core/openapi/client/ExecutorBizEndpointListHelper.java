package com.xxl.job.core.openapi.client;

import com.xxl.tool.core.StringTool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class ExecutorBizEndpointListHelper {

    public static List<String> split(String rawAddressList) {
        List<String> result = new ArrayList<String>();
        if (StringTool.isBlank(rawAddressList)) {
            return result;
        }

        for (String item : rawAddressList.split(",")) {
            if (StringTool.isNotBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    public static List<String> normalizeList(Collection<String> endpoints) {
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        if (endpoints == null) {
            return new ArrayList<String>();
        }

        for (String endpoint : endpoints) {
            if (StringTool.isBlank(endpoint)) {
                continue;
            }
            normalized.add(ExecutorBizClientTransportFactory.normalizeEndpoint(endpoint));
        }
        return new ArrayList<String>(normalized);
    }

    public static String normalizeAndJoin(String rawAddressList) {
        return join(normalizeList(split(rawAddressList)));
    }

    public static String join(Collection<String> endpoints) {
        List<String> normalized = normalizeList(endpoints);
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(",", normalized);
    }

    private ExecutorBizEndpointListHelper() {
    }
}
