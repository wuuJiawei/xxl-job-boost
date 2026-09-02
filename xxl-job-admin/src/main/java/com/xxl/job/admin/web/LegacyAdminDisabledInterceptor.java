package com.xxl.job.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/** The legacy Freemarker console is intentionally unavailable; admin-next is the only UI entry. */
public class LegacyAdminDisabledInterceptor implements HandlerInterceptor {
    private static final Set<String> LEGACY_PAGE_PATHS = Set.of(
            "/", "/dashboard", "/help", "/jobinfo", "/joblog", "/jobgroup", "/user", "/jobcode", "/auth/login");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("GET".equalsIgnoreCase(request.getMethod()) && LEGACY_PAGE_PATHS.contains(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        return true;
    }
}
