package com.xxl.job.core.openapi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExecutorBizClientTransportRegistry {

    private static final CopyOnWriteArrayList<ExecutorBizClientTransport> TRANSPORTS = new CopyOnWriteArrayList<ExecutorBizClientTransport>();

    static {
        for (ExecutorBizClientTransport transport : ServiceLoader.load(ExecutorBizClientTransport.class)) {
            register(transport);
        }
    }

    public static void register(ExecutorBizClientTransport transport) {
        if (transport == null) {
            return;
        }
        for (int i = 0; i < TRANSPORTS.size(); i++) {
            ExecutorBizClientTransport current = TRANSPORTS.get(i);
            if (current.type().equalsIgnoreCase(transport.type())) {
                TRANSPORTS.set(i, transport);
                return;
            }
        }
        TRANSPORTS.add(transport);
    }

    public static List<ExecutorBizClientTransport> list() {
        return new ArrayList<ExecutorBizClientTransport>(TRANSPORTS);
    }

    private ExecutorBizClientTransportRegistry() {
    }
}
