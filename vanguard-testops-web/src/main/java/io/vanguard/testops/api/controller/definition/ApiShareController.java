package io.vanguard.testops.api.controller.definition;

import io.vanguard.testops.api.dto.definition.ApiDefinitionDocDTO;
import io.vanguard.testops.api.dto.definition.ApiDefinitionDocRequest;
import io.vanguard.testops.api.dto.share.ShareInfoDTO;
import io.vanguard.testops.api.service.ApiShareService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@RestController
@RequestMapping(value = "/api/share")
@Tag(name = "接口测试-接口管理-接口分享")
public class ApiShareController {

    @Resource
    private ApiShareService apiShareService;

    @PostMapping("/doc/gen")
    @Operation(summary = "接口测试-接口管理-接口文档分享")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public ShareInfoDTO genApiDocShareInfo(@RequestBody ApiDefinitionDocRequest request) {
        return apiShareService.genApiDocShareInfo(request, Objects.requireNonNull(SessionUtils.getUser()));
    }

    @GetMapping("/doc/view/{shareId}")
    @Operation(summary = "接口测试-接口管理-接口文档分享查看")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    @CheckOwner(resourceId = "#shareId", resourceType = "share_info")
    public ApiDefinitionDocDTO shareDocView(@PathVariable String shareId) {
        return apiShareService.shareDocView(shareId);
    }
}
