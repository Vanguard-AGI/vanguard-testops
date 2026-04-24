package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiScenario;
import io.vanguard.testops.api.domain.ApiScenarioExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApiScenarioMapper {
    long countByExample(ApiScenarioExample example);

    int deleteByExample(ApiScenarioExample example);

    int deleteByPrimaryKey(String id);

    int insert(ApiScenario record);

    int insertSelective(ApiScenario record);

    List<ApiScenario> selectByExample(ApiScenarioExample example);

    ApiScenario selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") ApiScenario record, @Param("example") ApiScenarioExample example);

    int updateByExample(@Param("record") ApiScenario record, @Param("example") ApiScenarioExample example);

    int updateByPrimaryKeySelective(ApiScenario record);

    int updateByPrimaryKey(ApiScenario record);

    int batchInsert(@Param("list") List<ApiScenario> list);

    int batchInsertSelective(@Param("list") List<ApiScenario> list, @Param("selective") ApiScenario.Column ... selective);
}