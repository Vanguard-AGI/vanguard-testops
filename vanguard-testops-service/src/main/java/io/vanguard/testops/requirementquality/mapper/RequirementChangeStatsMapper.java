package io.vanguard.testops.requirementquality.mapper;

import io.vanguard.testops.requirementquality.domain.RequirementChangeStats;
import io.vanguard.testops.requirementquality.request.PipelineRecordListRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 需求-流水线变更事实表 requirement_change_stats（spotter_efficiency）
 */
@Mapper
public interface RequirementChangeStatsMapper {

    int insert(@Param("row") RequirementChangeStats row);

    RequirementChangeStats selectById(@Param("id") String id);

    List<RequirementChangeStats> selectListPage(@Param("request") PipelineRecordListRequest request, @Param("offset") long offset);

    long selectCount(@Param("request") PipelineRecordListRequest request);

    int updateById(@Param("row") RequirementChangeStats row);
}
