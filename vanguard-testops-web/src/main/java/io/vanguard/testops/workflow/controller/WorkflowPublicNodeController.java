package io.vanguard.testops.workflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodeDTO;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodePageRequest;
import io.vanguard.testops.workflow.dto.WorkflowPublicNodeSaveRequest;
import io.vanguard.testops.workflow.service.WorkflowPublicNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流公共节点 Controller
 * 提供公共节点的 CRUD 接口
 */
@Tag(name = "工作流公共节点管理")
@RestController
@RequestMapping("/workflow/public-node")
public class WorkflowPublicNodeController {

    @Resource
    private WorkflowPublicNodeService publicNodeService;

    /**
     * 保存公共节点（创建或更新）
     */
    @PostMapping("/save")
    @Operation(summary = "保存公共节点")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @CheckOwner(resourceId = "#request.projectId", resourceType = "project")
    public WorkflowPublicNodeDTO save(@Validated @RequestBody WorkflowPublicNodeSaveRequest request) {
        String userId = SessionUtils.getUserId();
        return publicNodeService.savePublicNode(request, userId);
    }

    /**
     * 分页查询公共节点列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询公共节点列表")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    @CheckOwner(resourceId = "#request.projectId", resourceType = "project")
    public IPage<WorkflowPublicNodeDTO> page(@Validated @RequestBody WorkflowPublicNodePageRequest request) {
        return publicNodeService.getPublicNodePage(request);
    }

    /**
     * 查询公共节点列表（不分页，用于前端直接展示）
     */
    @GetMapping("/list")
    @Operation(summary = "查询公共节点列表")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public List<WorkflowPublicNodeDTO> list(@RequestParam String projectId, @RequestParam(required = false) String category) {
        return publicNodeService.getPublicNodeList(projectId, category);
    }

    /**
     * 删除公共节点（软删除）
     */
    @GetMapping("/delete/{id}")
    @Operation(summary = "删除公共节点")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_DELETE)
    public void delete(@PathVariable String id, @RequestParam String projectId) {
        String userId = SessionUtils.getUserId();
        publicNodeService.deletePublicNode(id, projectId, userId);
    }
}

