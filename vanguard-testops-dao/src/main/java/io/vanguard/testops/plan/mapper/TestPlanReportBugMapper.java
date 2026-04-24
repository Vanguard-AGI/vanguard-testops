package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanReportBug;
import io.vanguard.testops.plan.domain.TestPlanReportBugExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TestPlanReportBugMapper {
    long countByExample(TestPlanReportBugExample example);

    int deleteByExample(TestPlanReportBugExample example);

    int deleteByPrimaryKey(String id);

    int insert(TestPlanReportBug record);

    int insertSelective(TestPlanReportBug record);

    List<TestPlanReportBug> selectByExample(TestPlanReportBugExample example);

    TestPlanReportBug selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") TestPlanReportBug record, @Param("example") TestPlanReportBugExample example);

    int updateByExample(@Param("record") TestPlanReportBug record, @Param("example") TestPlanReportBugExample example);

    int updateByPrimaryKeySelective(TestPlanReportBug record);

    int updateByPrimaryKey(TestPlanReportBug record);

    int batchInsert(@Param("list") List<TestPlanReportBug> list);

    int batchInsertSelective(@Param("list") List<TestPlanReportBug> list, @Param("selective") TestPlanReportBug.Column ... selective);
}