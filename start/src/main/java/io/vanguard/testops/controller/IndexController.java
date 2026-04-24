package io.vanguard.testops.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    @GetMapping("/")
    public String index() {
        // 前端未构建时，重定向到 Swagger UI
        return "redirect:/swagger-ui/index.html";
    }


    @GetMapping(value = "/login")
    public String login() {
        // 前端未构建时，重定向到 Swagger UI
        return "redirect:/swagger-ui/index.html";
    }
}
