package com.xxl.job.admin.controller.base;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the modern admin SPA entry while keeping the legacy console intact.
 */
@Controller
public class AdminNextController {

    @GetMapping({
            "/admin-next",
            "/admin-next/",
            "/admin-next/{path:[^\\.]*}",
            "/admin-next/{path:[^\\.]*}/{subpath:[^\\.]*}",
            "/admin-next/{path:[^\\.]*}/{subpath:[^\\.]*}/{tail:[^\\.]*}"
    })
    public String index() {
        return "forward:/static/admin-next/index.html";
    }
}
