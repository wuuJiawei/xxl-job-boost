package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobAuditLogMapper;
import com.xxl.job.admin.model.XxlJobAuditLog;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.json.GsonTool;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private XxlJobAuditLogMapper xxlJobAuditLogMapper;

    @Override
    public void record(LoginInfo loginInfo,
                       HttpServletRequest request,
                       String actionType,
                       String resourceType,
                       String resourceId,
                       String resourceName,
                       Integer jobGroup,
                       Object detail) {
        save(loginInfo != null ? loginInfo.getUserId() : null,
                loginInfo != null ? loginInfo.getUserName() : "unknown",
                actionType,
                resourceType,
                resourceId,
                resourceName,
                jobGroup,
                detail,
                request != null ? request.getRequestURI() : null,
                request != null ? request.getMethod() : null,
                resolveSource(request),
                resolveClientIp(request));
    }

    @Override
    public void recordSystem(String operatorUserId,
                             String operatorName,
                             String source,
                             String actionType,
                             String resourceType,
                             String resourceId,
                             String resourceName,
                             Integer jobGroup,
                             Object detail) {
        save(operatorUserId,
                operatorName,
                actionType,
                resourceType,
                resourceId,
                resourceName,
                jobGroup,
                detail,
                null,
                null,
                source,
                null);
    }

    private void save(String operatorUserId,
                      String operatorName,
                      String actionType,
                      String resourceType,
                      String resourceId,
                      String resourceName,
                      Integer jobGroup,
                      Object detail,
                      String requestPath,
                      String requestMethod,
                      String source,
                      String clientIp) {
        XxlJobAuditLog auditLog = new XxlJobAuditLog();
        auditLog.setOperatorUserId(operatorUserId);
        auditLog.setOperator(operatorName);
        auditLog.setActionType(actionType);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResourceName(resourceName);
        auditLog.setJobGroup(jobGroup);
        auditLog.setDetailJson(detail != null ? GsonTool.toJson(detail) : null);
        auditLog.setRequestPath(requestPath);
        auditLog.setRequestMethod(requestMethod);
        auditLog.setSource(source);
        auditLog.setClientIp(clientIp);
        auditLog.setCreateTime(new Date());
        xxlJobAuditLogMapper.save(auditLog);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveSource(HttpServletRequest request) {
        if (request == null) {
            return "system";
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/api/admin-next/")) {
            return "admin-next";
        }
        return "legacy-admin";
    }
}
