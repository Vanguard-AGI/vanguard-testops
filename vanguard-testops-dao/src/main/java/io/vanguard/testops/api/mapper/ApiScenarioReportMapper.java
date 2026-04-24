package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiScenarioReport;
import io.vanguard.testops.api.domain.ApiScenarioReportExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApiScenarioReportMapper {
    long countByExample(ApiScenarioReportExample example);

    int deleteByExample(ApiScenarioReportExample example);

    int deleteByPrimaryKey(String id);

    int insert(ApiScenarioReport record);

    int insertSelective(ApiScenarioReport record);

    List<ApiScenarioReport> selectByExample(ApiScenarioReportExample example);

    ApiScenarioReport selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") ApiScenarioReport record, @Param("example") ApiScenarioReportExample example);

    int updateByExample(@Param("record") ApiScenarioReport record, @Param("example") ApiScenarioReportExample example);

    int updateByPrimaryKeySelective(ApiScenarioReport record);

    int updateByPrimaryKey(ApiScenarioReport record);

    int batchInsert(@Param("list") List<ApiScenarioReport> list);

    int batchInsertSelective(@Param("list") List<ApiScenarioReport> list, @Param("selective") ApiScenarioReport.Column ... selective);
}