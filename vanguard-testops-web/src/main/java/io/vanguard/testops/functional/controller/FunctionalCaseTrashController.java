package io.vanguard.testops.functional.controller;

import com.alibaba.excel.util.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.functional.dto.FunctionalCasePageDTO;
import io.vanguard.testops.functional.request.FunctionalCaseBatchRequest;
import io.vanguard.testops.functional.request.FunctionalCasePageRequest;
import io.vanguard.testops.functional.service.FunctionalCaseLogService;
import io.vanguard.testops.functional.service.FunctionalCaseService;
import io.vanguard.testops.functional.service.FunctionalCaseTrashService;
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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用例管理-功能用例-回收站")
@RestController
@RequestMapping("/functional/case/trash")
public class FunctionalCaseTrashController {
    @Resource
    private FunctionalCaseTrashService functionalCaseTrashService;

    @Resource
    private FunctionalCaseService functionalCaseService;

    @PostMapping("/page")
    @Operation(summary = "用例管理-功能用例-回收站-用例列表查询")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public Pager<List<FunctionalCasePageDTO>> getFunctionalCasePage(@Validated @RequestBody FunctionalCasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "delete_time desc, id asc");
        return PageUtils.setPageInfo(page, functionalCaseService.getFunctionalCasePage(request, true, true));
    }

    @PostMapping("/module/count")
    @Operation(summary = "用例管理-功能用例-回收站-模块树统计用例数量")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public Map<String, Long> moduleCount(@Validated @RequestBody FunctionalCasePageRequest request) {
        return functionalCaseService.moduleCount(request, true);
    }

    @GetMapping("/recover/{id}")
    @Operation(summary = "用例管理-功能用例-回收站-恢复用例")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_DELETE)
    @Log(type = OperationLogType.RECOVER, expression = "#msClass.recoverLog(#id)", msClass = FunctionalCaseLogService.class)
    @CheckOwner(resourceId = "#id", resourceType = "functional_case")
    public void recoverCase(@PathVariable String id) {
        functionalCaseTrashService.recoverCase(id, SessionUtils.getUserId());
    }

    @PostMapping("/batch/recover")
    @Operation(summary = "用例管理-功能用例-回收站-批量恢复用例")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_DELETE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.RECOVER, expression = "#msClass.batchRecoverLog(#request)", msClass = FunctionalCaseLogService.class)
    public void batchRecoverCase(@Validated @RequestBody FunctionalCaseBatchRequest request) {
        functionalCaseTrashService.batchRecoverCase(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "用例管理-功能用例-回收站-彻底删除用例")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteTrashCaseLog(#id)", msClass = FunctionalCaseLogService.class)
    @CheckOwner(resourceId = "#id", resourceType = "functional_case")
    public void deleteCase(@PathVariable String id) {
        functionalCaseTrashService.deleteCase(id, SessionUtils.getUserId());
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "用例管理-功能用例-回收站-批量彻底删除用例")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_DELETE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.batchDeleteTrashCaseLog(#request)", msClass = FunctionalCaseLogService.class)
    public void batchDeleteCase(@Validated @RequestBody FunctionalCaseBatchRequest request) {
        functionalCaseTrashService.batchDeleteCase(request, SessionUtils.getUserId());
    }

}
