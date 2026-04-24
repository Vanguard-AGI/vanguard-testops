package io.vanguard.testops.plan.controller;

import io.vanguard.testops.plan.constants.TestPlanResourceConfig;
import io.vanguard.testops.plan.dto.request.TestPlanBatchExecuteRequest;
import io.vanguard.testops.plan.dto.request.TestPlanExecuteRequest;
import io.vanguard.testops.plan.service.TestPlanExecuteService;
import io.vanguard.testops.plan.service.TestPlanLogService;
import io.vanguard.testops.plan.service.TestPlanManagementService;
import io.vanguard.testops.project.service.ProjectService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.dto.user.UserExtendDTO;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/test-plan-execute")
@Tag(name = "测试计划执行")
public class TestPlanExecuteController {

    @Resource
    private TestPlanManagementService testPlanManagementService;
    @Resource
    private TestPlanExecuteService testPlanExecuteService;
    @Resource
    private ProjectService projectService;

    private static final String NULL_KEY = "-";

    @PostMapping("/single")
    @Operation(summary = "测试计划单独执行")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_READ_EXECUTE)
    @CheckOwner(resourceId = "#request.getExecuteId()", resourceType = "test_plan")
    @Log(type = OperationLogType.EXECUTE, expression = "#msClass.executeLog(#request)", msClass = TestPlanLogService.class)
    public String startExecute(@Validated @RequestBody TestPlanExecuteRequest request) {
        testPlanManagementService.checkModuleIsOpen(request.getExecuteId(), TestPlanResourceConfig.CONFIG_TEST_PLAN, Collections.singletonList(TestPlanResourceConfig.CONFIG_TEST_PLAN));
        String reportId = IDGenerator.nextStr();
        Thread.startVirtualThread(() ->
                testPlanExecuteService.singleExecuteTestPlan(request, reportId, SessionUtils.getUserId())
        );
        return reportId;
    }

    @PostMapping("/batch")
    @Operation(summary = "测试计划-批量执行")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_READ_EXECUTE)
    @CheckOwner(resourceId = "#request.getExecuteIds()", resourceType = "test_plan")
    @Log(type = OperationLogType.EXECUTE, expression = "#msClass.batchExecuteLog(#request)", msClass = TestPlanLogService.class)
    public void startExecute(@Validated @RequestBody TestPlanBatchExecuteRequest request) {
        testPlanManagementService.checkModuleIsOpen(request.getProjectId(), TestPlanResourceConfig.CHECK_TYPE_PROJECT, Collections.singletonList(TestPlanResourceConfig.CONFIG_TEST_PLAN));
        Thread.startVirtualThread(() ->
                testPlanExecuteService.batchExecuteTestPlan(request, SessionUtils.getUserId())
        );
    }

    @GetMapping("/user-option/{projectId}")
    @Operation(summary = "执行人下拉选项(空选项)")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public List<UserExtendDTO> getMemberOption(@PathVariable String projectId,
                                               @Schema(description = "查询关键字，根据邮箱和用户名查询")
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        List<UserExtendDTO> memberOption = projectService.getMemberOption(projectId, keyword);
        // 空选项
        if (StringUtils.isBlank(keyword) || StringUtils.equals(keyword, NULL_KEY)) {
            UserExtendDTO userExtendDTO = new UserExtendDTO();
            userExtendDTO.setId(NULL_KEY);
            userExtendDTO.setName(NULL_KEY);
            memberOption.add(userExtendDTO);
        }
        return memberOption;
    }
}
