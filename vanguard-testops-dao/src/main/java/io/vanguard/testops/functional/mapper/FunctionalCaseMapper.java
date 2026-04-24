package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FunctionalCaseMapper {
    long countByExample(FunctionalCaseExample example);

    int deleteByExample(FunctionalCaseExample example);

    int deleteByPrimaryKey(String id);

    int insert(FunctionalCase record);

    int insertSelective(FunctionalCase record);

    List<FunctionalCase> selectByExample(FunctionalCaseExample example);

    FunctionalCase selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") FunctionalCase record, @Param("example") FunctionalCaseExample example);

    int updateByExample(@Param("record") FunctionalCase record, @Param("example") FunctionalCaseExample example);

    int updateByPrimaryKeySelective(FunctionalCase record);

    int updateByPrimaryKey(FunctionalCase record);

    int batchInsert(@Param("list") List<FunctionalCase> list);

    int batchInsertSelective(@Param("list") List<FunctionalCase> list, @Param("selective") FunctionalCase.Column ... selective);
}