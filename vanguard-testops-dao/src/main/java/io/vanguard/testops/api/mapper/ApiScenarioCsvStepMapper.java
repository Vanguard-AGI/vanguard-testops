package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiScenarioCsvStep;
import io.vanguard.testops.api.domain.ApiScenarioCsvStepExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApiScenarioCsvStepMapper {
    long countByExample(ApiScenarioCsvStepExample example);

    int deleteByExample(ApiScenarioCsvStepExample example);

    int deleteByPrimaryKey(String id);

    int insert(ApiScenarioCsvStep record);

    int insertSelective(ApiScenarioCsvStep record);

    List<ApiScenarioCsvStep> selectByExample(ApiScenarioCsvStepExample example);

    ApiScenarioCsvStep selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") ApiScenarioCsvStep record, @Param("example") ApiScenarioCsvStepExample example);

    int updateByExample(@Param("record") ApiScenarioCsvStep record, @Param("example") ApiScenarioCsvStepExample example);

    int updateByPrimaryKeySelective(ApiScenarioCsvStep record);

    int updateByPrimaryKey(ApiScenarioCsvStep record);

    int batchInsert(@Param("list") List<ApiScenarioCsvStep> list);

    int batchInsertSelective(@Param("list") List<ApiScenarioCsvStep> list, @Param("selective") ApiScenarioCsvStep.Column ... selective);
}