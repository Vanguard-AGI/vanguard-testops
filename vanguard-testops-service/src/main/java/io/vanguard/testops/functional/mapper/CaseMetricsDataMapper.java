package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseMetricsData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CaseMetricsDataMapper {
    
    int insert(CaseMetricsData record);
    
    int insertSelective(CaseMetricsData record);
    
    CaseMetricsData selectByPrimaryKey(String id);
    
    int updateByPrimaryKeySelective(CaseMetricsData record);
    
    int updateByPrimaryKey(CaseMetricsData record);
    
    int deleteByPrimaryKey(String id);
    
    CaseMetricsData selectLatestByProject(@Param("projectId") String projectId, 
                                          @Param("metricLevel") String metricLevel, 
                                          @Param("timeDimension") String timeDimension);
    
    List<CaseMetricsData> selectByProjectAndTimeRange(@Param("projectId") String projectId, 
                                                       @Param("startTime") Long startTime, 
                                                       @Param("endTime") Long endTime);
    
    int batchInsert(@Param("list") List<CaseMetricsData> list);
}
