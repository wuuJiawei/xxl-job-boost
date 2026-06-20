package com.xxl.job.admin.controller.base;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves admin-next static assets under the servlet context path.
 */
@Controller
public class AdminNextStaticController {

    @GetMapping("/admin-next/favicon.svg")
    public String favicon() {
        return "forward:/static/admin-next/favicon.svg";
    }

    @GetMapping("/admin-next/assets/{name:.+}")
    public String assets(@PathVariable String name) {
        return "forward:/static/admin-next/assets/" + name;
    }
}
