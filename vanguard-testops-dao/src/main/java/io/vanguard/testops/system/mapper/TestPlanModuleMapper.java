package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.TestPlanModule;
import io.vanguard.testops.system.domain.TestPlanModuleExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TestPlanModuleMapper {
    long countByExample(TestPlanModuleExample example);

    int deleteByExample(TestPlanModuleExample example);

    int deleteByPrimaryKey(String id);

    int insert(TestPlanModule record);

    int insertSelective(TestPlanModule record);

    List<TestPlanModule> selectByExample(TestPlanModuleExample example);

    TestPlanModule selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") TestPlanModule record, @Param("example") TestPlanModuleExample example);

    int updateByExample(@Param("record") TestPlanModule record, @Param("example") TestPlanModuleExample example);

    int updateByPrimaryKeySelective(TestPlanModule record);

    int updateByPrimaryKey(TestPlanModule record);

    int batchInsert(@Param("list") List<TestPlanModule> list);

    int batchInsertSelective(@Param("list") List<TestPlanModule> list, @Param("selective") TestPlanModule.Column ... selective);
}