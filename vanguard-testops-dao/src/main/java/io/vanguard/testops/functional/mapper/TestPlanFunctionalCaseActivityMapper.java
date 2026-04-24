package io.vanguard.testops.functional.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用例执行活跃度：spotter_aegis.test_plan_functional_case 按 execute_user、last_exec_time 统计
 * 用于 /activity 的 moduleType 维度中的「用例执行」
 */
public interface TestPlanFunctionalCaseActivityMapper {

    /**
     * 查询时间范围内有执行记录的 execute_user 列表（每条执行记录一条，用于在 Java 中按用户分组计数）
     *
     * @param lastExecTimeStart 最后执行时间起始（毫秒）
     * @param lastExecTimeEnd   最后执行时间结束（毫秒）
     * @param projectIds       项目 ID 列表（可选，通过 test_plan 关联）
     * @param executeUserIds   执行人 ID 列表（可选，personal 时传入）
     * @return execute_user 列表，可能重复
     */
    List<String> selectExecuteUserIdsInRange(@Param("lastExecTimeStart") Long lastExecTimeStart,
                                              @Param("lastExecTimeEnd") Long lastExecTimeEnd,
                                              @Param("projectIds") List<String> projectIds,
                                              @Param("executeUserIds") List<String> executeUserIds);

    /**
     * 功能用例执行总数：test_plan_functional_case 在时间范围内有执行记录（last_exec_time 非空且在范围内），支持 projectIds、executeUser
     */
    long countExecutionInRange(@Param("projectIds") List<String> projectIds,
                               @Param("executeUser") String executeUser,
                               @Param("lastExecTimeStart") Long lastExecTimeStart,
                               @Param("lastExecTimeEnd") Long lastExecTimeEnd);

    /**
     * 功能用例执行成功数：同上，且 last_exec_result = 'SUCCESS'
     */
    long countSuccessInRange(@Param("projectIds") List<String> projectIds,
                             @Param("executeUser") String executeUser,
                             @Param("lastExecTimeStart") Long lastExecTimeStart,
                             @Param("lastExecTimeEnd") Long lastExecTimeEnd);
}
