package com.xxl.job.core.server;

import com.xxl.tool.core.StringTool;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExecutorTransportRegistry {

    private static final String DEFAULT_TRANSPORT = "NETTY_EMBED";
    private static final CopyOnWriteArrayList<ExecutorTransport> TRANSPORTS = new CopyOnWriteArrayList<ExecutorTransport>();

    static {
        register(new NettyEmbedExecutorTransport());
        register(new SpringHttpExecutorTransport());
        for (ExecutorTransport transport : ServiceLoader.load(ExecutorTransport.class)) {
            register(transport);
        }
    }

    public static void register(ExecutorTransport transport) {
        if (transport == null || StringTool.isBlank(transport.type())) {
            return;
        }

        for (int i = 0; i < TRANSPORTS.size(); i++) {
            ExecutorTransport current = TRANSPORTS.get(i);
            if (current.type().equalsIgnoreCase(transport.type())) {
                TRANSPORTS.set(i, transport);
                return;
            }
        }
        TRANSPORTS.add(transport);
    }

    public static ExecutorTransport get(String transportType) {
        String finalTransportType = StringTool.isBlank(transportType) ? DEFAULT_TRANSPORT : transportType.trim();
        for (ExecutorTransport transport : TRANSPORTS) {
            if (transport.type().equalsIgnoreCase(finalTransportType)) {
                return transport;
            }
        }
        throw new IllegalArgumentException("Unsupported executor transport: " + transportType);
    }

    public static List<ExecutorTransport> list() {
        return new ArrayList<ExecutorTransport>(TRANSPORTS);
    }

    private ExecutorTransportRegistry() {
    }
}
