package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseExecutionRecord;
import io.vanguard.testops.functional.domain.CaseExecutionRecordAggregate;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CaseExecutionRecordMapper {
    
    int deleteByPrimaryKey(String id);
    
    int insert(CaseExecutionRecord record);
    
    int insertSelective(CaseExecutionRecord record);
    
    CaseExecutionRecord selectByPrimaryKey(String id);
    
    int updateByPrimaryKeySelective(CaseExecutionRecord record);
    
    int updateByPrimaryKey(CaseExecutionRecord record);
    
    List<CaseExecutionRecord> selectByCaseId(@Param("caseId") String caseId);
    
    List<CaseExecutionRecord> selectByCaseIdAndTimeRange(@Param("caseId") String caseId,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime);
    
    List<CaseExecutionRecord> selectByProjectId(@Param("projectId") String projectId, 
                                                @Param("startTime") Long startTime, 
                                                @Param("endTime") Long endTime);
    
    List<CaseExecutionRecord> selectByPlanId(@Param("planId") String planId);
    
    int countByCaseId(@Param("caseId") String caseId);
    
    int countByProjectIdAndTimeRange(@Param("projectId") String projectId, 
                                      @Param("startTime") Long startTime, 
                                      @Param("endTime") Long endTime);
    
    int batchInsert(@Param("list") List<CaseExecutionRecord> list);

    CaseExecutionRecordAggregate selectExecutionSummaryByProject(@Param("projectId") String projectId,
                                                                 @Param("startTime") Long startTime,
                                                                 @Param("endTime") Long endTime);

    CaseExecutionRecordAggregate selectExecutionSummaryByCaseIds(@Param("caseIds") List<String> caseIds,
                                                                 @Param("startTime") Long startTime,
                                                                 @Param("endTime") Long endTime);

    List<Map<String, Object>> selectExecutionCountsByProject(@Param("projectId") String projectId,
                                                             @Param("startTime") Long startTime,
                                                             @Param("endTime") Long endTime);

    List<Map<String, Object>> selectExecutionCountsByCaseIds(@Param("caseIds") List<String> caseIds,
                                                             @Param("startTime") Long startTime,
                                                             @Param("endTime") Long endTime);
}

