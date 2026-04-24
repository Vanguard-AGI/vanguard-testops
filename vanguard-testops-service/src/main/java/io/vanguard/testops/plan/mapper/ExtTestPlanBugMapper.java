package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.dto.TestPlanBugCaseDTO;
import io.vanguard.testops.plan.dto.TestPlanCaseBugDTO;
import io.vanguard.testops.plan.dto.request.TestPlanBugPageRequest;
import io.vanguard.testops.plan.dto.response.TestPlanBugPageResponse;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtTestPlanBugMapper {

    /**
     * 查询计划-关联缺陷列表
     *
     * @param request 请求参数
     * @return 缺陷列表
     */
    @BaseConditionFilter
    List<TestPlanBugPageResponse> list(@Param("request") TestPlanBugPageRequest request);

    /**
     * 根据缺陷ID集合获取计划下缺陷关联的用例集合
     *
     * @param bugIds 缺陷ID集合
     * @param planId 计划ID
     * @return 用例集合
     */
    List<TestPlanBugCaseDTO> getBugRelatedCase(@Param("ids") List<String> bugIds, @Param("planId") String planId);

    List<TestPlanBugCaseDTO> getBugRelatedCaseByCaseIds(@Param("ids") List<String> bugIds, @Param("caseIds") List<String> caseIds, @Param("planId") String planId);


    List<TestPlanBugPageResponse> countBugByIds(@Param("planIds") List<String> planIds);

    List<TestPlanBugPageResponse> selectBugCountByPlanId(@Param("planId") String planId);

    List<TestPlanBugPageResponse> selectPlanRelationBug(@Param("planId") String planId);

    /**
     * 根据用例关系ID集合获取计划下用例关联的缺陷集合
     *
     * @param caseIds 用例ID集合
     * @return 缺陷集合
     */
    List<TestPlanCaseBugDTO> getCaseRelatedBug(@Param("ids") List<String> caseIds);
}
