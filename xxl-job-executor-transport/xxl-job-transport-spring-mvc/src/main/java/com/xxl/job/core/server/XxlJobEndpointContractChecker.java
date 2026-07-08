package com.xxl.job.core.server;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("removal")
public class XxlJobEndpointContractChecker implements SmartInitializingSingleton {

    private final RequestMappingHandlerMapping mapping;
    private final ExecutorEndpointAdapter adapter;

    public XxlJobEndpointContractChecker(RequestMappingHandlerMapping mapping, ExecutorEndpointAdapter adapter) {
        this.mapping = mapping;
        this.adapter = adapter;
    }

    @Override
    public void afterSingletonsInstantiated() {
        adapter.assertSupportedAllRequiredEndpoints();

        Set<String> actualPaths = mapping.getHandlerMethods()
                .keySet()
                .stream()
                .filter(this::isPostMapping)
                .flatMap(this::paths)
                .collect(Collectors.toSet());

        Set<String> requiredPaths = Arrays.stream(ExecutorEndpoint.values())
                .map(ExecutorEndpoint::path)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> missing = new LinkedHashSet<>(requiredPaths);
        missing.removeAll(actualPaths);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing XXL-JOB executor endpoints: " + missing);
        }
    }

    private boolean isPostMapping(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        return methods.isEmpty() || methods.contains(RequestMethod.POST);
    }

    private Stream<String> paths(RequestMappingInfo info) {
        PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            return pathPatternsCondition.getPatterns()
                    .stream()
                    .map(PathPattern::getPatternString);
        }

        PatternsRequestCondition patternsCondition = info.getPatternsCondition();
        if (patternsCondition != null) {
            return patternsCondition.getPatterns().stream();
        }

        return Stream.empty();
    }
}
