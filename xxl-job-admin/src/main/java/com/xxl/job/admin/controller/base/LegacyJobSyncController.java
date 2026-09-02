package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.service.LegacyJobSyncService;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-next/legacy-sync")
public class LegacyJobSyncController {
    @Resource
    private LegacyJobSyncService legacyJobSyncService;

    @PostMapping("/preview")
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> preview(@RequestBody SyncRequest request) {
        try {
            return Response.ofSuccess(legacyJobSyncService.preview(request.sourceUrl(), request.username(), request.password()));
        } catch (Exception e) {
            return Response.ofFail(message(e));
        }
    }

    @PostMapping("/import")
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> importJobs(@RequestBody SyncRequest request, HttpServletRequest httpRequest) {
        try {
            LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(httpRequest).getData();
            return Response.ofSuccess(legacyJobSyncService.importJobs(request.sourceUrl(), request.username(), request.password(),
                    request.jobIds(), loginInfo, httpRequest));
        } catch (Exception e) {
            return Response.ofFail(message(e));
        }
    }

    private String message(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? "旧平台同步失败" : exception.getMessage();
    }

    public record SyncRequest(String sourceUrl, String username, String password, List<Integer> jobIds) {
    }
}
