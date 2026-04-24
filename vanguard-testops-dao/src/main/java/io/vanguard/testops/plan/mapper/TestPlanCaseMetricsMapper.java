package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanCaseMetrics;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TestPlanCaseMetricsMapper {
    int insert(TestPlanCaseMetrics record);

    int updateByPrimaryKey(TestPlanCaseMetrics record);

    TestPlanCaseMetrics selectByPrimaryKey(String id);

    TestPlanCaseMetrics selectByPlanAndCase(@Param("testPlanId") String testPlanId, @Param("caseId") String caseId);

    List<TestPlanCaseMetrics> selectByTestPlanId(String testPlanId);
    
    /**
     * 查询指定测试计划在指定日期范围内的批量提交用例
     * @param testPlanId 测试计划ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 批量提交的用例列表
     */
    List<TestPlanCaseMetrics> selectBatchCasesByPlanAndDate(
            @Param("testPlanId") String testPlanId, 
            @Param("startTime") Long startTime, 
            @Param("endTime") Long endTime);
}
