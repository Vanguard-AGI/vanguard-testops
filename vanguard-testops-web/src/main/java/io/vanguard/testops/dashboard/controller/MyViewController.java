package io.vanguard.testops.dashboard.controller;

import com.alibaba.excel.util.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.api.dto.definition.ApiTestCaseDTO;
import io.vanguard.testops.api.dto.definition.ApiTestCasePageRequest;
import io.vanguard.testops.api.dto.scenario.ApiScenarioDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioPageRequest;
import io.vanguard.testops.api.service.ApiTestService;
import io.vanguard.testops.api.service.definition.ApiTestCaseService;
import io.vanguard.testops.api.service.scenario.ApiScenarioService;
import io.vanguard.testops.bug.dto.request.BugPageRequest;
import io.vanguard.testops.bug.dto.response.BugDTO;
import io.vanguard.testops.bug.service.BugService;
import io.vanguard.testops.dashboard.request.DashboardViewApiCaseTableRequest;
import io.vanguard.testops.dashboard.request.DashboardViewPlanTableRequest;
import io.vanguard.testops.dashboard.request.DashboardViewTableRequest;
import io.vanguard.testops.functional.dto.CaseReviewDTO;
import io.vanguard.testops.functional.dto.FunctionalCasePageDTO;
import io.vanguard.testops.functional.request.CaseReviewPageRequest;
import io.vanguard.testops.functional.request.FunctionalCasePageRequest;
import io.vanguard.testops.functional.service.CaseReviewService;
import io.vanguard.testops.functional.service.FunctionalCaseService;
import io.vanguard.testops.plan.dto.request.TestPlanTableRequest;
import io.vanguard.testops.plan.dto.response.TestPlanResponse;
import io.vanguard.testops.plan.dto.response.TestPlanStatisticsResponse;
import io.vanguard.testops.plan.service.TestPlanManagementService;
import io.vanguard.testops.plan.service.TestPlanStatisticsService;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.dto.ProtocolDTO;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Jan
 */

@Tag(name = "工作台-我的", description = "前端视图ID传参(viewId): {关注: 'my_follow', 创建: 'my_create'}")
@RestController
@RequestMapping("/dashboard/my")
public class MyViewController {

	@Resource
	private TestPlanManagementService testPlanManagementService;
	@Resource
	private TestPlanStatisticsService testPlanStatisticsService;
	@Resource
	private FunctionalCaseService functionalCaseService;
	@Resource
	private CaseReviewService caseReviewService;
	@Resource
	private ApiTestCaseService apiTestCaseService;
	@Resource
	private ApiScenarioService apiScenarioService;
	@Resource
	private ApiTestService apiTestService;
	@Resource
	private BugService bugService;

	@PostMapping("/plan/page")
	@Operation(summary = "我的-测试计划-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<TestPlanResponse>> page(@Validated @RequestBody DashboardViewPlanTableRequest viewRequest) {
		return testPlanManagementService.page(buildTargetRequest(new TestPlanTableRequest(), viewRequest));
	}

	@PostMapping("/plan/statistics")
	@Operation(summary = "我的-测试计划-获取列表详情统计 {通过率, 执行进度}")
	@Parameter(name = "ids", description = "计划ID集合", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
	public List<TestPlanStatisticsResponse> selectTestPlanMetricById(@RequestBody List<String> ids) {
		return testPlanStatisticsService.calculateRate(ids);
	}

	@PostMapping("/functional/page")
	@Operation(summary = "我的-测试用例-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<FunctionalCasePageDTO>> getFunctionalCasePage(@Validated @RequestBody DashboardViewTableRequest viewRequest) {
		Page<Object> page = PageHelper.startPage(viewRequest.getCurrent(), viewRequest.getPageSize(),
				StringUtils.isNotBlank(viewRequest.getSortString()) ? viewRequest.getSortString() : "pos desc");
		return PageUtils.setPageInfo(page, functionalCaseService.getFunctionalCasePage(buildTargetRequest(new FunctionalCasePageRequest(), viewRequest), false, true));
	}

	@PostMapping("/review/page")
	@Operation(summary = "我的-用例评审-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<CaseReviewDTO>> getCaseReviewPage(@Validated @RequestBody DashboardViewTableRequest viewRequest) {
		Page<Object> page = PageHelper.startPage(viewRequest.getCurrent(), viewRequest.getPageSize(),
				StringUtils.isNotBlank(viewRequest.getSortString()) ? viewRequest.getSortString() : "pos desc");
		return PageUtils.setPageInfo(page, caseReviewService.getCaseReviewPage(buildTargetRequest(new CaseReviewPageRequest(), viewRequest)));
	}

	@PostMapping(value = "/api/page")
	@Operation(summary = "我的-接口用例-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<ApiTestCaseDTO>> page(@Validated @RequestBody DashboardViewApiCaseTableRequest viewRequest) {
		// 默认查询当前用户组织下的所有协议
		List<ProtocolDTO> protocols = apiTestService.getProtocols(SessionUtils.getCurrentOrganizationId());
		List<String> protocolList = protocols.stream().map(ProtocolDTO::getProtocol).toList();
		viewRequest.setProtocols(protocolList);
		Page<Object> page = PageHelper.startPage(viewRequest.getCurrent(), viewRequest.getPageSize(),
				StringUtils.isNotBlank(viewRequest.getSortString("id")) ? viewRequest.getSortString("id") : "pos desc, id desc");
		return PageUtils.setPageInfo(page, apiTestCaseService.page(buildTargetRequest(new ApiTestCasePageRequest(), viewRequest), false, true, null));
	}

	@PostMapping("/scenario/page")
	@Operation(summary = "我的-接口场景-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<ApiScenarioDTO>> getPage(@Validated @RequestBody DashboardViewTableRequest viewRequest) {
		Page<Object> page = PageHelper.startPage(viewRequest.getCurrent(), viewRequest.getPageSize(),
				StringUtils.isNotBlank(viewRequest.getSortString("id")) ? viewRequest.getSortString("id") : "pos desc, id desc");
		return PageUtils.setPageInfo(page, apiScenarioService.getScenarioPage(buildTargetRequest(new ApiScenarioPageRequest(), viewRequest), true, null));
	}

	@PostMapping("/bug/page")
	@Operation(summary = "我的-缺陷-列表分页查询")
	@CheckOwner(resourceId = "#viewRequest.getProjectId()", resourceType = "project")
	public Pager<List<BugDTO>> page(@Validated @RequestBody DashboardViewTableRequest viewRequest) {
		Page<Object> page = PageHelper.startPage(viewRequest.getCurrent(), viewRequest.getPageSize(),
				StringUtils.isNotBlank(viewRequest.getSortString()) ? viewRequest.getSortString() : "pos desc");
		BugPageRequest targetRequest = buildTargetRequest(new BugPageRequest(), viewRequest);
		targetRequest.setUseTrash(false);
		return PageUtils.setPageInfo(page, bugService.list(targetRequest));
	}

	/**
	 * 解析工作台通用的列表视图参数=>各个模块的分页参数
	 * @param targetRequest 目标模块的分页参数
	 * @param viewRequest 通用的列表视图参数
	 * @return 目标模块的分页参数
	 */
	private <R, S extends DashboardViewTableRequest> R buildTargetRequest(R targetRequest, S viewRequest) {
		BeanUtils.copyBean(targetRequest, viewRequest);
		return targetRequest;
	}
}
