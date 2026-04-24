package io.vanguard.testops.plan.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.api.dto.definition.ApiReportDTO;
import io.vanguard.testops.api.dto.definition.ApiReportDetailDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioReportDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioReportDetailDTO;
import io.vanguard.testops.api.service.definition.ApiReportService;
import io.vanguard.testops.api.service.scenario.ApiScenarioReportService;
import io.vanguard.testops.bug.dto.response.BugDTO;
import io.vanguard.testops.plan.constants.AssociateCaseType;
import io.vanguard.testops.plan.domain.TestPlanReportComponent;
import io.vanguard.testops.plan.dto.ReportDetailCasePageDTO;
import io.vanguard.testops.plan.dto.TestPlanShareInfo;
import io.vanguard.testops.plan.dto.request.TestPlanReportShareRequest;
import io.vanguard.testops.plan.dto.request.TestPlanShareReportDetailRequest;
import io.vanguard.testops.plan.dto.response.TestPlanCaseExecHistoryResponse;
import io.vanguard.testops.plan.dto.response.TestPlanReportDetailCollectionResponse;
import io.vanguard.testops.plan.dto.response.TestPlanReportDetailResponse;
import io.vanguard.testops.plan.dto.response.TestPlanShareResponse;
import io.vanguard.testops.plan.service.TestPlanReportService;
import io.vanguard.testops.plan.service.TestPlanReportShareService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.domain.ShareInfo;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-plan/report/share")
@Tag(name = "测试计划-分享")
public class TestPlanReportShareController {

    @Resource
    private TestPlanReportService testPlanReportService;
    @Resource
    private TestPlanReportShareService testPlanReportShareService;
    @Resource
    private ApiReportService apiReportService;
    @Resource
    private ApiScenarioReportService apiScenarioReportService;

    @PostMapping("/gen")
    @Operation(summary = "测试计划-报告-分享")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_REPORT_READ_SHARE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public TestPlanShareInfo genReportShareInfo(@RequestBody TestPlanReportShareRequest request) {
        return testPlanReportShareService.gen(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "测试计划-报告-获取分享链接")
    public TestPlanShareResponse get(@PathVariable String id) {
        return testPlanReportShareService.get(id);
    }

    @GetMapping("/get-share-time/{id}")
    @Operation(summary = "测试计划-报告-获取分享链接的有效时间")
    public String getShareTime(@PathVariable String id) {
        return testPlanReportShareService.getShareTime(id);
    }

    // 分享报告详情开始

    @GetMapping("/get-layout/{shareId}/{reportId}")
    @Operation(summary = "测试计划-报告-组件布局")
    public List<TestPlanReportComponent> getLayout(@PathVariable String shareId, @PathVariable String reportId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return testPlanReportService.getLayout(reportId);
    }

    @GetMapping("/get/detail/{shareId}/{reportId}")
    @Operation(summary = "测试计划-报告分享-详情查看")
    public TestPlanReportDetailResponse getDetail(@PathVariable String shareId, @PathVariable String reportId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return testPlanReportService.getReport(reportId);
    }

    @PostMapping("/detail/bug/page")
    @Operation(summary = "测试计划-报告-详情-缺陷分页查询")
    public Pager<List<BugDTO>> pageBug(@Validated @RequestBody TestPlanShareReportDetailRequest request) {
        request.setDetailReportIds(testPlanReportService.getActualReportIds(request.getReportId()));
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "tprb.bug_num desc");
        if (!request.getStartPager()) {
            page.close();
            page.setOrderBy("tprb.bug_num desc");
        }
        return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailBugs(request));
    }

    @PostMapping("/detail/functional/case/page")
    @Operation(summary = "测试计划-报告-详情-功能用例分页查询")
    public Pager<List<ReportDetailCasePageDTO>> pageFunctionalCase(@Validated @RequestBody TestPlanShareReportDetailRequest request) {
        request.setDetailReportIds(testPlanReportService.getActualReportIds(request.getReportId()));
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        String sort = request.getSortString();
        sort = StringUtils.replace(sort, "request_time", "request_duration");
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(sort) ? sort : "tprfc.pos desc");
        if (!request.getStartPager()) {
            // 不分页仅排序 {测试集升序, 用例位次倒序}
            page.setPageSize(0);
            page.setPageSizeZero(true);
            page.setOrderBy("tpc.pos, tpc.name, tprfc.pos desc");
            page.setOrderByOnly(true);
        }
        return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailCases(request, null, AssociateCaseType.FUNCTIONAL));
    }

    @PostMapping("/detail/api/case/page")
    @Operation(summary = "测试计划-报告-详情-接口用例分页查询")
    public Pager<List<ReportDetailCasePageDTO>> pageApiCase(@Validated @RequestBody TestPlanShareReportDetailRequest request) {
        request.setDetailReportIds(testPlanReportService.getActualReportIds(request.getReportId()));
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        String sort = request.getSortString();
        sort = StringUtils.replace(sort, "request_time", "request_duration");
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(sort) ? sort : "tprac.pos desc");
        if (!request.getStartPager()) {
            // 不分页仅排序 {测试集升序, 用例位次倒序}
            page.setPageSize(0);
            page.setPageSizeZero(true);
            page.setOrderBy("tpc.pos, tpc.name, tprac.pos desc");
            page.setOrderByOnly(true);
        }
        return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailCases(request, null, AssociateCaseType.API_CASE));
    }

    @PostMapping("/detail/scenario/case/page")
    @Operation(summary = "测试计划-报告-详情-场景用例分页查询")
    public Pager<List<ReportDetailCasePageDTO>> pageScenarioCase(@Validated @RequestBody TestPlanShareReportDetailRequest request) {
        request.setDetailReportIds(testPlanReportService.getActualReportIds(request.getReportId()));
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        String sort = request.getSortString();
        sort = StringUtils.replace(sort, "request_time", "request_duration");
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(sort) ? sort : "tpras.pos desc");
        if (!request.getStartPager()) {
            // 不分页仅排序 {测试集升序, 用例位次倒序}
            page.setPageSize(0);
            page.setPageSizeZero(true);
            page.setOrderBy("tpc.pos, tpc.name, tpras.pos desc");
            page.setOrderByOnly(true);
        }
        return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailCases(request, null, AssociateCaseType.API_SCENARIO));
    }

    @PostMapping("/detail/plan/report/page")
    @Operation(summary = "测试计划-报告-集合报告详情")
    public Pager<List<TestPlanReportDetailResponse>> planReportPage(@Validated @RequestBody TestPlanShareReportDetailRequest request) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "tpr.create_time desc");
        if (!request.getStartPager()) {
            page.close();
        }
        return PageUtils.setPageInfo(page, testPlanReportService.planReportList(request));
    }

    @GetMapping("/detail/api-report/{shareId}/{reportId}")
    @Operation(summary = "测试计划-接口用例-查看报告")
    public ApiReportDTO getApiReport(@PathVariable String shareId, @PathVariable String reportId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return apiReportService.get(reportId);
    }

    @GetMapping("/detail/api-report/get/{shareId}/{reportId}/{stepId}")
    @Operation(summary = "测试计划-接口用例-查看报告详情")
    public List<ApiReportDetailDTO> getReportContent(@PathVariable String shareId,
                                                     @PathVariable String reportId,
                                                     @PathVariable String stepId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return apiReportService.getDetail(reportId, stepId);
    }

    @GetMapping("/detail/scenario-report/{shareId}/{reportId}")
    @Operation(summary = "测试计划-接口场景-查看报告")
    public ApiScenarioReportDTO getScenarioReport(@PathVariable String shareId, @PathVariable String reportId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return apiScenarioReportService.get(reportId);
    }

    @GetMapping("/detail/scenario-report/get/{shareId}/{reportId}/{stepId}")
    @Operation(summary = "测试计划-接口场景-查看报告详情")
    public List<ApiScenarioReportDetailDTO> selectReportContent(@PathVariable String shareId,
                                                                @PathVariable String reportId,
                                                                @PathVariable String stepId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return apiScenarioReportService.getDetail(reportId, stepId);
    }

    @GetMapping("/detail/functional/case/step/{shareId}/{reportId}")
    @Operation(summary = "测试计划-报告-详情-功能用例-执行步骤结果")
    public TestPlanCaseExecHistoryResponse getFunctionalExecuteResult(@PathVariable String shareId, @PathVariable String reportId) {
        ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
        testPlanReportShareService.validateExpired(shareInfo);
        return testPlanReportService.getFunctionalExecuteResult(reportId);
    }

    @PostMapping("/detail/{type}/collection/page")
    @Operation(summary = "测试计划-报告-详情-测试集分页查询(不同用例类型)")
    @Parameter(name = "type", description = "用例类型", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED), example = "functional, api, scenario")
    public Pager<List<TestPlanReportDetailCollectionResponse>> collectionPage(@PathVariable String type, @Validated @RequestBody TestPlanShareReportDetailRequest request) {
        request.setDetailReportIds(testPlanReportService.getActualReportIds(request.getReportId()));
        ShareInfo shareInfo = testPlanReportShareService.checkResource(request.getShareId());
        testPlanReportShareService.validateExpired(shareInfo);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(), "tpc.pos asc");
        return PageUtils.setPageInfo(page, testPlanReportService.listReportCollection(request, type));
    }
}
