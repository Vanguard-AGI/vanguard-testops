package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanMetrics;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TestPlanMetricsMapper {
    int insert(TestPlanMetrics record);

    int updateByPrimaryKey(TestPlanMetrics record);

    TestPlanMetrics selectByPrimaryKey(String id);

    TestPlanMetrics selectByTestPlanId(String testPlanId);
}
