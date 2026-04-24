package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiDefinitionSwagger;
import io.vanguard.testops.api.domain.ApiDefinitionSwaggerExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApiDefinitionSwaggerMapper {
    long countByExample(ApiDefinitionSwaggerExample example);

    int deleteByExample(ApiDefinitionSwaggerExample example);

    int deleteByPrimaryKey(String id);

    int insert(ApiDefinitionSwagger record);

    int insertSelective(ApiDefinitionSwagger record);

    List<ApiDefinitionSwagger> selectByExample(ApiDefinitionSwaggerExample example);

    ApiDefinitionSwagger selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") ApiDefinitionSwagger record, @Param("example") ApiDefinitionSwaggerExample example);

    int updateByExample(@Param("record") ApiDefinitionSwagger record, @Param("example") ApiDefinitionSwaggerExample example);

    int updateByPrimaryKeySelective(ApiDefinitionSwagger record);

    int updateByPrimaryKey(ApiDefinitionSwagger record);

    int batchInsert(@Param("list") List<ApiDefinitionSwagger> list);

    int batchInsertSelective(@Param("list") List<ApiDefinitionSwagger> list, @Param("selective") ApiDefinitionSwagger.Column ... selective);
}