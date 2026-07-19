package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.model.EmailSettings;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.job.admin.service.SystemConfigService;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-next/system-settings")
public class SystemSettingsController {

    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/email")
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> emailSettings() {
        return Response.ofSuccess(toSafeView(systemConfigService.getEmailSettings()));
    }

    @PostMapping("/email")
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> updateEmailSettings(HttpServletRequest request,
                                                             @RequestBody EmailSettings settings) {
        try {
            EmailSettings updated = systemConfigService.updateEmailSettings(settings);
            Map<String, Object> safeView = toSafeView(updated);
            LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(request).getData();
            auditLogService.record(loginInfo, request, "system-settings-update", "system-settings",
                    "email", "邮箱设置", null, safeView);
            return Response.ofSuccess(safeView);
        } catch (IllegalArgumentException e) {
            return Response.ofFail(e.getMessage());
        }
    }

    private Map<String, Object> toSafeView(EmailSettings settings) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", settings.isEnabled());
        data.put("host", settings.getHost());
        data.put("port", settings.getPort());
        data.put("username", settings.getUsername());
        data.put("from", settings.getFrom());
        data.put("password", "");
        data.put("passwordConfigured", settings.isPasswordConfigured());
        data.put("smtpAuth", settings.isSmtpAuth());
        data.put("starttlsEnabled", settings.isStarttlsEnabled());
        data.put("starttlsRequired", settings.isStarttlsRequired());
        data.put("sslEnabled", settings.isSslEnabled());
        return data;
    }
}
