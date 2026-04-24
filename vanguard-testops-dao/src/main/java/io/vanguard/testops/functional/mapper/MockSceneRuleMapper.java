package io.vanguard.testops.functional.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * Mock 工厂指标：spotter_runner.mock_scene_rule 总数统计（deleted_at IS NULL，支持按时间查询）
 */
public interface MockSceneRuleMapper {

    /**
     * 统计未删除的规则数量，可选按 created_at 时间范围筛选
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 数量
     */
    long countByDeletedAtIsNull(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
