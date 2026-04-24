package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.api.domain.ApiTestCaseExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ApiTestCaseMapper {
    long countByExample(ApiTestCaseExample example);

    int deleteByExample(ApiTestCaseExample example);

    int deleteByPrimaryKey(String id);

    int insert(ApiTestCase record);

    int insertSelective(ApiTestCase record);

    List<ApiTestCase> selectByExample(ApiTestCaseExample example);

    ApiTestCase selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") ApiTestCase record, @Param("example") ApiTestCaseExample example);

    int updateByExample(@Param("record") ApiTestCase record, @Param("example") ApiTestCaseExample example);

    int updateByPrimaryKeySelective(ApiTestCase record);

    int updateByPrimaryKey(ApiTestCase record);

    int batchInsert(@Param("list") List<ApiTestCase> list);

    int batchInsertSelective(@Param("list") List<ApiTestCase> list, @Param("selective") ApiTestCase.Column ... selective);
}