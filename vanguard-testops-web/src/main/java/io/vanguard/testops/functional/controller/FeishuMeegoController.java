package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.domain.MeegoStoryStats;
import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 飞书Meego需求查询接口
 */
@Tag(name = "用例管理-飞书需求")
@RestController
@RequestMapping("/functional/case/feishu")
public class FeishuMeegoController {

    @Resource
    private FeishuMeegoService feishuMeegoService;

    @GetMapping("/story/search")
    @Operation(summary = "用例管理-搜索飞书需求")
    @RequiresPermissions(value = {PermissionConstants.FUNCTIONAL_CASE_READ, PermissionConstants.FUNCTIONAL_CASE_READ_ADD, PermissionConstants.FUNCTIONAL_CASE_READ_UPDATE}, logical = Logical.OR)
    public List<MeegoStoryStats> searchStories(@RequestParam(required = false, defaultValue = "") String keyword) {
        return feishuMeegoService.searchStories(keyword);
    }
}
