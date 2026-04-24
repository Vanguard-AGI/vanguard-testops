package io.vanguard.testops.functional.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试计划指标：spotter_aegis.test_plan 按 group_id 去重且排除 NONE 的总数统计
 * 查询条件与 metadata_definition 一致：projectIds、create_user、create_time 时间范围
 */
public interface TestPlanStatsMapper {

    /**
     * 统计去重 group_id 数量（排除 group_id = 'NONE' 及空值），可选条件与 metadata_definition 一致
     *
     * @param projectIds    项目 ID 列表（可选）
     * @param createUser    创建人（可选）
     * @param createTimeStart 创建时间起始（毫秒，可选）
     * @param createTimeEnd   创建时间结束（毫秒，可选）
     * @return 去重后的 group_id 数量
     */
    long countDistinctGroupIdExcludingNone(@Param("projectIds") List<String> projectIds,
                                          @Param("createUser") String createUser,
                                          @Param("createTimeStart") Long createTimeStart,
                                          @Param("createTimeEnd") Long createTimeEnd);
}
