package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanReportApiScenario;
import io.vanguard.testops.plan.dto.CaseStatusCountMap;
import io.vanguard.testops.plan.dto.ReportDetailCasePageDTO;
import io.vanguard.testops.plan.dto.TestPlanBaseModule;
import io.vanguard.testops.plan.dto.request.TestPlanReportDetailPageRequest;
import io.vanguard.testops.plan.dto.response.TestPlanReportDetailCollectionResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtTestPlanReportApiScenarioMapper {

	/**
	 * 统计报告中场景用例执行情况
	 * @param reportIds 报告ID集合
	 * @return 用例数量
	 */
	List<CaseStatusCountMap> countExecuteResult(@Param("ids") List<String> reportIds);

	/**
	 * 获取计划关联的场景用例
	 * @param planId 计划ID
	 * @return 场景用例列表
	 */
	List<TestPlanReportApiScenario> getPlanExecuteCases(@Param("id") String planId, @Param("ids") List<String> ids);


	List<String> getPlanExecuteCasesId(@Param("id") String planId);

	/**
	 * 获取项目下场景用例所属模块集合
	 * @param ids 模块ID集合
	 * @return 模块集合
	 */
	List<TestPlanBaseModule> getPlanExecuteCaseModules(@Param("ids") List<String> ids);

	/**
	 * 分页查询报告关联的用例
	 * @param request 请求参数
	 * @return 关联的用例集合
	 */
	List<ReportDetailCasePageDTO> list(@Param("request") TestPlanReportDetailPageRequest request, @Param("sort") String sort);

	/**
	 * 分页查询报告关联的测试集(场景)
	 * @param request 请求参数
	 * @return 关联的测试集集合
	 */
	List<TestPlanReportDetailCollectionResponse> listCollection(@Param("request") TestPlanReportDetailPageRequest request);

	List<String> selectExecResultByReportIdAndCollectionId(@Param("collectionId") String collectionId, @Param("reportId") String prepareReportId);
}
