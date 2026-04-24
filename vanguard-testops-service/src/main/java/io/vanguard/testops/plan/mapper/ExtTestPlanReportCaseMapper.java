package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanReportApiCase;
import io.vanguard.testops.plan.domain.TestPlanReportApiScenario;

import java.util.List;

public interface ExtTestPlanReportCaseMapper {

    List<String> selectFunctionalCaseIdsByTestPlanId(String testPlanId);

    List<TestPlanReportApiCase> selectApiCaseIdAndResultByReportId(String testPlanReportId);

    List<TestPlanReportApiScenario> selectApiScenarioIdAndResultByReportId(String testPlanReportId);
}
