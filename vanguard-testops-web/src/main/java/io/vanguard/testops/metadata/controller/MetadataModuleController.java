package io.vanguard.testops.metadata.controller;

import io.vanguard.testops.metadata.dto.MetadataModuleCreateRequest;
import io.vanguard.testops.metadata.dto.MetadataModuleUpdateRequest;
import io.vanguard.testops.metadata.service.MetadataModuleService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据模块控制器
 */
@RestController
@RequestMapping("/metadata/module")
@Tag(name = "元数据管理-模块管理")
public class MetadataModuleController {

    @Resource
    private MetadataModuleService metadataModuleService;

    @PostMapping("/add")
    @Operation(summary = "创建模块")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    // @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public String add(@Validated @RequestBody MetadataModuleCreateRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        return metadataModuleService.create(request, userId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新模块")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "")
    public void update(@Validated @RequestBody MetadataModuleUpdateRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        metadataModuleService.update(request, userId);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除模块")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "")
    public void delete(@PathVariable String id) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        metadataModuleService.delete(id, userId);
    }

    @GetMapping("/tree/{projectId}")
    @Operation(summary = "获取模块树")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    // @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public List<BaseTreeNode> getTree(
            @PathVariable String projectId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) String moduleType) {
        return metadataModuleService.getTree(projectId, typeId, moduleType);
    }

    @PostMapping("/init/{projectId}")
    @Operation(summary = "初始化根目录")
    public void initRootModules(@PathVariable String projectId) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        metadataModuleService.initRootModules(projectId, userId);
    }
}
