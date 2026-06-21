package com.xxl.job.admin.controller.base;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.StringUtils;

/**
 * Serves admin-next static assets under the servlet context path.
 */
@Controller
public class AdminNextStaticController {
    private static final String ASSET_ROUTE_PREFIX = "/admin-next/assets/";
    private static final String ASSET_CLASSPATH_PREFIX = "static/admin-next/assets/";

    @GetMapping("/admin-next/favicon.svg")
    public ResponseEntity<Resource> favicon() {
        return serveResource("static/admin-next/", "favicon.svg");
    }

    @GetMapping("/admin-next/assets/**")
    public ResponseEntity<Resource> assets(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestPath = request.getRequestURI().substring((contextPath + ASSET_ROUTE_PREFIX).length());
        return serveResource(ASSET_CLASSPATH_PREFIX, requestPath);
    }

    private ResponseEntity<Resource> serveResource(String basePath, String relativePath) {
        String cleanPath = StringUtils.cleanPath(relativePath);
        if (!StringUtils.hasText(cleanPath) || cleanPath.startsWith("..") || cleanPath.contains("../")) {
            return ResponseEntity.notFound().build();
        }

        ClassPathResource resource = new ClassPathResource(basePath + cleanPath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(cleanPath).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
