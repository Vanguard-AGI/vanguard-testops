package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.dto.dashboard.PersonalStatsDTO;
import io.vanguard.testops.functional.dto.dashboard.ProjectOverviewDTO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface MetricDashboardMapper {
    
    List<ProjectOverviewDTO> selectProjectOverview(@Param("projectId") String projectId, @Param("userId") String userId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    List<PersonalStatsDTO> selectPersonalStats(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);
}
