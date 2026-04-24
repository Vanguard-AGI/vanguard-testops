package io.vanguard.testops.plan.service;

import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.api.domain.ApiTestCaseExample;
import io.vanguard.testops.api.mapper.ApiTestCaseMapper;
import io.vanguard.testops.plan.domain.TestPlan;
import io.vanguard.testops.plan.domain.TestPlanApiCase;
import io.vanguard.testops.plan.domain.TestPlanApiCaseExample;
import io.vanguard.testops.plan.dto.request.TestPlanApiCaseBatchMoveRequest;
import io.vanguard.testops.plan.dto.request.TestPlanApiCaseUpdateRequest;
import io.vanguard.testops.plan.mapper.TestPlanApiCaseMapper;
import io.vanguard.testops.plan.mapper.TestPlanMapper;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.service.OperationLogService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanApiCaseLogService {

    @Resource
    private TestPlanApiCaseService testPlanApiCaseService;
    @Resource
    private TestPlanApiCaseMapper testPlanApiCaseMapper;
    @Resource
    private ApiTestCaseMapper apiTestCaseMapper;
    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private OperationLogService operationLogService;

    public void batchUpdateExecutor(TestPlanApiCaseUpdateRequest request) {
        try {
            List<String> ids = testPlanApiCaseService.doSelectIds(request);
            if (CollectionUtils.isNotEmpty(ids)) {
                TestPlan testPlan = testPlanMapper.selectByPrimaryKey(request.getTestPlanId());
                Project project = projectMapper.selectByPrimaryKey(testPlan.getProjectId());
                TestPlanApiCaseExample example = new TestPlanApiCaseExample();
                example.createCriteria().andIdIn(ids);
                List<TestPlanApiCase> planCaseList = testPlanApiCaseMapper.selectByExample(example);
                Map<String, String> userMap = planCaseList.stream().collect(Collectors.toMap(TestPlanApiCase::getId, TestPlanApiCase::getExecuteUser));
                Map<String, String> idsMap = planCaseList.stream().collect(Collectors.toMap(TestPlanApiCase::getId, TestPlanApiCase::getApiCaseId));
                List<String> caseIds = planCaseList.stream().map(TestPlanApiCase::getApiCaseId).collect(Collectors.toList());
                ApiTestCaseExample caseExample = new ApiTestCaseExample();
                caseExample.createCriteria().andIdIn(caseIds);
                List<ApiTestCase> functionalCases = apiTestCaseMapper.selectByExample(caseExample);
                Map<String, String> caseMap = functionalCases.stream().collect(Collectors.toMap(ApiTestCase::getId, ApiTestCase::getName));
                List<LogDTO> dtoList = new ArrayList<>();
                idsMap.forEach((k, v) -> {
                    LogDTO dto = new LogDTO(
                            project.getId(),
                            project.getOrganizationId(),
                            k,
                            request.getUserId(),
                            OperationLogType.UPDATE.name(),
                            OperationLogModule.TEST_PLAN,
                            Translator.get("test_plan.update.executor") + ":" + caseMap.get(v));
                    dto.setPath("/test-plan/api/case/batch/update/executor");
                    dto.setMethod(HttpMethodConstants.POST.name());
                    dto.setOriginalValue(JSON.toJSONBytes(userMap.get(k)));
                    dto.setModifiedValue(JSON.toJSONBytes(request.getUserId()));
                    dtoList.add(dto);
                });
                operationLogService.batchAdd(dtoList);
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    public void batchMove(TestPlanApiCaseBatchMoveRequest request, String userId) {
        try {
            List<String> ids = testPlanApiCaseService.doSelectIds(request);
            if (CollectionUtils.isNotEmpty(ids)) {
                TestPlan testPlan = testPlanMapper.selectByPrimaryKey(request.getTestPlanId());
                Project project = projectMapper.selectByPrimaryKey(testPlan.getProjectId());
                TestPlanApiCaseExample example = new TestPlanApiCaseExample();
                example.createCriteria().andIdIn(ids);
                List<TestPlanApiCase> caseList = testPlanApiCaseMapper.selectByExample(example);
                List<String> apiCaseIds = caseList.stream().map(TestPlanApiCase::getApiCaseId).collect(Collectors.toList());
                ApiTestCaseExample caseExample = new ApiTestCaseExample();
                caseExample.createCriteria().andIdIn(apiCaseIds);
                List<ApiTestCase> apiTestCases = apiTestCaseMapper.selectByExample(caseExample);
                Map<String, String> caseMap = apiTestCases.stream().collect(Collectors.toMap(ApiTestCase::getId, ApiTestCase::getName));
                List<LogDTO> dtoList = new ArrayList<>();
                caseList.forEach(item -> {
                    LogDTO dto = new LogDTO(
                            project.getId(),
                            project.getOrganizationId(),
                            item.getApiCaseId(),
                            userId,
                            OperationLogType.UPDATE.name(),
                            OperationLogModule.TEST_PLAN,
                            Translator.get("move") + ":" + caseMap.get(item.getApiCaseId()));
                    dto.setPath("/test-plan/api/case/batch/move");
                    dto.setMethod(HttpMethodConstants.POST.name());
                    dto.setOriginalValue(JSON.toJSONBytes(item));
                    TestPlanApiCase testPlanApiCase = new TestPlanApiCase();
                    testPlanApiCase.setId(item.getId());
                    testPlanApiCase.setTestPlanCollectionId(request.getTargetCollectionId());
                    dto.setModifiedValue(JSON.toJSONBytes(testPlanApiCase));
                    dtoList.add(dto);
                });
                operationLogService.batchAdd(dtoList);
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }
}



