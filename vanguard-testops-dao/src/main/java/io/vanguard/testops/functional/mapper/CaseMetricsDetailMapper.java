package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CaseMetricsDetailMapper {
    int insert(CaseMetricsDetail record);

    int updateByPrimaryKey(CaseMetricsDetail record);

    CaseMetricsDetail selectByPrimaryKey(String id);

    CaseMetricsDetail selectByCaseId(String caseId);

    List<CaseMetricsDetail> selectByProjectId(String projectId);
    
    /**
     * 批量查询用例指标详情（性能优化）
     */
    List<CaseMetricsDetail> selectByCaseIds(@Param("caseIds") List<String> caseIds);
    
    /**
     * 批量插入用例指标详情（性能优化）
     */
    int batchInsert(@Param("list") List<CaseMetricsDetail> list);
}
