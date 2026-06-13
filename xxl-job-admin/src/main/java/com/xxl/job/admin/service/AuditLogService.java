package com.xxl.job.admin.service;

import com.xxl.sso.core.model.LoginInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {

    void record(LoginInfo loginInfo,
                HttpServletRequest request,
                String actionType,
                String resourceType,
                String resourceId,
                String resourceName,
                Integer jobGroup,
                Object detail);

    void recordSystem(String operatorUserId,
                      String operatorName,
                      String source,
                      String actionType,
                      String resourceType,
                      String resourceId,
                      String resourceName,
                      Integer jobGroup,
                      Object detail);
}
