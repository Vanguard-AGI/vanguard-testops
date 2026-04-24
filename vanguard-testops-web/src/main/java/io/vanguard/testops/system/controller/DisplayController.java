package io.vanguard.testops.system.controller;

import io.vanguard.testops.system.dto.DisplayConfigDTO;
import io.vanguard.testops.system.dto.PageConfigResponse;
import io.vanguard.testops.system.service.DisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "页面配置")
@RestController
@RequestMapping(value = "/display")
public class DisplayController {

    @Resource
    private DisplayService displayService;

    @Operation(summary = "保存页面配置")
    @PostMapping("/save")
    public void saveDisplayConfig(
            @RequestParam("request") MultipartFile request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        displayService.saveDisplayConfig(request, files);
    }

    @Operation(summary = "获取页面配置")
    @GetMapping("/info")
    public List<PageConfigResponse> getDisplayConfig() {
        return displayService.getDisplayConfigList();
    }
}
