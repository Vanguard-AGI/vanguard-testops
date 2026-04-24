package io.vanguard.testops.bug.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.bug.dto.request.BugBatchRequest;
import io.vanguard.testops.bug.dto.request.BugPageRequest;
import io.vanguard.testops.bug.dto.response.BugDTO;
import io.vanguard.testops.bug.service.BugLogService;
import io.vanguard.testops.bug.service.BugService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "缺陷管理-回收站")
@RestController
@RequestMapping("/bug/trash")
public class BugTrashController {

    @Resource
    private BugService bugService;

    @PostMapping("/page")
    @Operation(summary = "缺陷管理-回收站-获取缺陷列表")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public Pager<List<BugDTO>> page(@Validated @RequestBody BugPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "delete_time desc, num asc");
        request.setUseTrash(true);
        return PageUtils.setPageInfo(page, bugService.list(request));
    }

    @GetMapping("/recover/{id}")
    @Operation(summary = "缺陷管理-回收站-恢复")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @Log(type = OperationLogType.RECOVER, expression = "#msClass.recoverLog(#id)", msClass = BugLogService.class)
    public void recover(@PathVariable String id) {
        bugService.recover(id);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "缺陷管理-回收站-彻底删除")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteTrashLog(#id)", msClass = BugLogService.class)
    public void deleteTrash(@PathVariable String id) {
        bugService.deleteTrash(id);
    }

    @PostMapping("/batch-recover")
    @Operation(summary = "缺陷管理-回收站-批量恢复")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public void batchRecover(@Validated @RequestBody BugBatchRequest request) {
        request.setUseTrash(true);
        bugService.batchRecover(request, SessionUtils.getUserId());
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "缺陷管理-回收站-批量彻底删除")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.batchDeleteTrashLog(#request)", msClass = BugLogService.class)
    public void batchDelete(@Validated @RequestBody BugBatchRequest request) {
        request.setUseTrash(true);
        bugService.batchDeleteTrash(request);
    }
}
