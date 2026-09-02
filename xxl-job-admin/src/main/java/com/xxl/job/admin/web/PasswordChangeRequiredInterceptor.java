package com.xxl.job.admin.web;

import com.xxl.job.admin.util.PasswordPolicy;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/** Blocks every authenticated operation until the user replaces the bootstrap password. */
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {
    private static final String API_PREFIX = "/api/";
    private static final String UPDATE_PASSWORD_PATH = "/auth/updatePwd";
    private static final String SESSION_PATH = "/api/admin-next/session";
    private static final String LOGOUT_PATH = "/auth/logout";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/admin-next") || path.equals("/auth/doLogin") || path.equals(LOGOUT_PATH)
                || path.equals(UPDATE_PASSWORD_PATH) || path.equals(SESSION_PATH)) {
            return true;
        }

        LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(request).getData();
        if (loginInfo == null || loginInfo.getExtraInfo() == null
                || !"true".equals(loginInfo.getExtraInfo().get("passwordChangeRequired"))) {
            return true;
        }

        if (path.startsWith(API_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(GsonTool.toJson(Response.ofFail(PasswordPolicy.validationMessage())));
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, PasswordPolicy.validationMessage());
        }
        return false;
    }
}
