package io.vanguard.testops.project.controller;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.log.service.OperationLogService;
import io.vanguard.testops.system.log.vo.OperationLogResponse;
import io.vanguard.testops.system.log.vo.ProOperationLogRequest;
import io.vanguard.testops.system.log.vo.SystemOperationLogRequest;
import io.vanguard.testops.system.service.SimpleUserService;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;


/**
 * @author Jan
 */

@Tag(name = "项目管理-日志")
@RestController
@RequestMapping("/project/log")
public class ProjectLogController {

    @Resource
    private SimpleUserService simpleUserService;

    @Resource
    private OperationLogService operationLogService;

    @GetMapping("/user/list/{projectId}")
    @Operation(summary = "项目管理-日志-获取用户列表-支持远程搜索")
    @RequiresPermissions(PermissionConstants.PROJECT_LOG_READ)
    public List<User> getUserList(@PathVariable(value = "projectId") String projectId,
                                  @Schema(description = "查询关键字，根据邮箱和用户名查询")
                                  @RequestParam(value = "keyword", required = false) String keyword) {
        return simpleUserService.getUserListByOrgId(StringUtils.defaultIfBlank(projectId, SessionUtils.getCurrentProjectId()), keyword);
    }


    @PostMapping("/list")
    @Operation(summary = "项目管理-日志--操作日志列表查询")
    @RequiresPermissions(PermissionConstants.PROJECT_LOG_READ)
    public Pager<List<OperationLogResponse>> projectLogList(@Validated @RequestBody ProOperationLogRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        SystemOperationLogRequest operationLogRequest = new SystemOperationLogRequest();
        BeanUtils.copyBean(operationLogRequest, request);
        operationLogRequest.setProjectIds(Arrays.asList(SessionUtils.getCurrentProjectId()));
        return PageUtils.setPageInfo(page, operationLogService.list(operationLogRequest));
    }

}
