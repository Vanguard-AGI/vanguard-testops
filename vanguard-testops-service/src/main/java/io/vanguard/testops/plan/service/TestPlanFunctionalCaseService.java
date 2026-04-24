package io.vanguard.testops.plan.service;

import com.alibaba.excel.util.BooleanUtils;
import io.vanguard.testops.api.domain.ApiScenario;
import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.bug.domain.BugRelationCase;
import io.vanguard.testops.bug.domain.BugRelationCaseExample;
import io.vanguard.testops.bug.dto.CaseRelateBugDTO;
import io.vanguard.testops.bug.mapper.BugMapper;
import io.vanguard.testops.bug.mapper.BugRelationCaseMapper;
import io.vanguard.testops.bug.mapper.ExtBugRelateCaseMapper;
import io.vanguard.testops.bug.service.BugStatusService;
import io.vanguard.testops.dto.BugProviderDTO;
import io.vanguard.testops.functional.constants.CaseFileSourceType;
import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseModule;
import io.vanguard.testops.functional.domain.FunctionalCaseTest;
import io.vanguard.testops.functional.dto.*;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseMapper;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseModuleMapper;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseTestMapper;
import io.vanguard.testops.functional.mapper.FunctionalCaseMapper;
import io.vanguard.testops.functional.service.FunctionalCaseAttachmentService;
import io.vanguard.testops.functional.service.FunctionalCaseModuleService;
import io.vanguard.testops.functional.service.FunctionalCaseService;
import io.vanguard.testops.plan.constants.AssociateCaseType;
import io.vanguard.testops.plan.constants.TreeTypeEnums;
import io.vanguard.testops.plan.domain.*;
import io.vanguard.testops.plan.dto.*;
import io.vanguard.testops.plan.dto.request.*;
import io.vanguard.testops.plan.dto.response.*;
import io.vanguard.testops.plan.mapper.*;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.MoveNodeSortDTO;
import io.vanguard.testops.provider.BaseAssociateBugProvider;
import io.vanguard.testops.request.AssociateBugPageRequest;
import io.vanguard.testops.sdk.constants.*;
import io.vanguard.testops.sdk.dto.AssociateCaseDTO;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.SubListUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.LogInsertModule;
import io.vanguard.testops.system.dto.ModuleSelectDTO;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.mapper.ExtUserMapper;
import io.vanguard.testops.system.notice.constants.NoticeConstants;
import io.vanguard.testops.system.service.UserLoginService;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanFunctionalCaseService extends TestPlanResourceService {
    @Resource
    private TestPlanFunctionalCaseMapper testPlanFunctionalCaseMapper;
    @Resource
    private ExtTestPlanFunctionalCaseMapper extTestPlanFunctionalCaseMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private TestPlanResourceLogService testPlanResourceLogService;
    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private FunctionalCaseService functionalCaseService;
    @Resource
    private UserLoginService userLoginService;
    @Resource
    private ExtBugRelateCaseMapper bugRelateCaseMapper;
    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private ExtTestPlanModuleMapper extTestPlanModuleMapper;
    @Resource
    private FunctionalCaseModuleService functionalCaseModuleService;
    @Resource
    private BaseAssociateBugProvider baseAssociateBugProvider;
    @Resource
    private BugMapper bugMapper;
    @Resource
    private TestPlanCaseExecuteHistoryMapper testPlanCaseExecuteHistoryMapper;
    @Resource
    private ExtTestPlanCaseExecuteHistoryMapper extTestPlanCaseExecuteHistoryMapper;
    @Resource
    private FunctionalCaseAttachmentService functionalCaseAttachmentService;
    @Resource
    private TestPlanSendNoticeService testPlanSendNoticeService;
    @Resource
    private BugStatusService bugStatusService;
    @Resource
    private TestPlanCollectionMapper testPlanCollectionMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtFunctionalCaseModuleMapper extFunctionalCaseModuleMapper;
    @Resource
    private ExtTestPlanCollectionMapper extTestPlanCollectionMapper;
    @Resource
    private TestPlanConfigService testPlanConfigService;
    @Resource
    private ExtFunctionalCaseMapper extFunctionalCaseMapper;
    @Resource
    private TestPlanApiCaseService testPlanApiCaseService;
    @Resource
    private ExtFunctionalCaseTestMapper extFunctionalCaseTestMapper;
    @Resource
    private TestPlanApiCaseMapper testPlanApiCaseMapper;
    @Resource
    private TestPlanApiScenarioService testPlanApiScenarioService;
    @Resource
    private TestPlanApiScenarioMapper testPlanApiScenarioMapper;
    
    @Resource
    private io.vanguard.testops.functional.mapper.CaseExecutionRecordMapper caseExecutionRecordMapper;
    @Resource
    private io.vanguard.testops.functional.service.CaseCSCalculationService caseCSCalculationService;
    @Resource
    private io.vanguard.testops.plan.mapper.TestPlanCaseMetricsMapper testPlanCaseMetricsMapper;
    @Resource
    private io.vanguard.testops.functional.mapper.CaseChangeLogMapper caseChangeLogMapper;
    @Resource
    private io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper caseMetricsDetailMapper;

    private static final String CASE_MODULE_COUNT_ALL = "all";
    private static final String EXECUTOR = "executeUserName";

    @Override
    public List<TestPlanResourceExecResultDTO> selectDistinctExecResultByProjectId(String projectId) {
        return extTestPlanFunctionalCaseMapper.selectDistinctExecResult(projectId);
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectLastExecResultByProjectId(String projectId) {
        return extTestPlanFunctionalCaseMapper.selectLastExecResultByProjectId(projectId);
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectDistinctExecResultByTestPlanIds(List<String> testPlanIds) {
        return extTestPlanFunctionalCaseMapper.selectDistinctExecResultByTestPlanIds(testPlanIds);
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectLastExecResultByTestPlanIds(List<String> testPlanIds) {
        return extTestPlanFunctionalCaseMapper.selectLastExecResultByTestPlanIds(testPlanIds);
    }

    @Override
    public long copyResource(String originalTestPlanId, String newTestPlanId, Map<String, String> oldCollectionIdToNewCollectionId, String operator, long operatorTime) {
        List<TestPlanFunctionalCase> copyList = new ArrayList<>();
        String defaultCollectionId = extTestPlanCollectionMapper.selectDefaultCollectionId(newTestPlanId, CaseType.SCENARIO_CASE.getKey());
        extTestPlanFunctionalCaseMapper.selectByTestPlanIdAndNotDeleted(originalTestPlanId).forEach(originalCase -> {
            TestPlanFunctionalCase newCase = new TestPlanFunctionalCase();
            BeanUtils.copyBean(newCase, originalCase);
            newCase.setId(IDGenerator.nextStr());
            newCase.setTestPlanId(newTestPlanId);
            newCase.setCreateTime(operatorTime);
            newCase.setCreateUser(operator);
            newCase.setLastExecTime(0L);
            newCase.setTestPlanCollectionId(oldCollectionIdToNewCollectionId.get(newCase.getTestPlanCollectionId()) == null ? defaultCollectionId : oldCollectionIdToNewCollectionId.get(newCase.getTestPlanCollectionId()));
            newCase.setLastExecResult(ExecStatus.PENDING.name());
            copyList.add(newCase);
        });

        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        TestPlanFunctionalCaseMapper batchInsertMapper = sqlSession.getMapper(TestPlanFunctionalCaseMapper.class);
        copyList.forEach(item -> batchInsertMapper.insert(item));
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        
        // Create metrics for copied cases
        copyList.forEach(this::createTestPlanCaseMetrics);
        
        return copyList.size();
    }

    @Override
    public void deleteBatchByTestPlanId(List<String> testPlanIdList) {
        if (CollectionUtils.isNotEmpty(testPlanIdList)) {
            TestPlanFunctionalCaseExample testPlanFunctionalCaseExample = new TestPlanFunctionalCaseExample();
            testPlanFunctionalCaseExample.createCriteria().andTestPlanIdIn(testPlanIdList);
            testPlanFunctionalCaseMapper.deleteByExample(testPlanFunctionalCaseExample);

            TestPlanCaseExecuteHistoryExample testPlanCaseExecuteHistoryExample = new TestPlanCaseExecuteHistoryExample();
            testPlanCaseExecuteHistoryExample.createCriteria().andTestPlanIdIn(testPlanIdList);
            testPlanCaseExecuteHistoryMapper.deleteByExample(testPlanCaseExecuteHistoryExample);
        }
    }


    @Override
    public long getNextOrder(String collectionId) {
        Long maxPos = extTestPlanFunctionalCaseMapper.getMaxPosByCollectionId(collectionId);
        if (maxPos == null) {
            return DEFAULT_NODE_INTERVAL_POS;
        } else {
            return maxPos + DEFAULT_NODE_INTERVAL_POS;
        }
    }

    @Override
    public void updatePos(String id, long pos) {
        extTestPlanFunctionalCaseMapper.updatePos(id, pos);
    }

    @Override
    public Map<String, Long> caseExecResultCount(String testPlanId) {
        List<TestPlanCaseRunResultCount> runResultCounts = extTestPlanFunctionalCaseMapper.selectCaseExecResultCount(testPlanId);
        return runResultCounts.stream().collect(Collectors.toMap(TestPlanCaseRunResultCount::getResult, TestPlanCaseRunResultCount::getResultCount));
    }

    @Override
    public void refreshPos(String testPlanId) {
        List<String> functionalCaseIdList = extTestPlanFunctionalCaseMapper.selectIdByTestPlanIdOrderByPos(testPlanId);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtTestPlanFunctionalCaseMapper batchUpdateMapper = sqlSession.getMapper(ExtTestPlanFunctionalCaseMapper.class);
        for (int i = 0; i < functionalCaseIdList.size(); i++) {
            batchUpdateMapper.updatePos(functionalCaseIdList.get(i), i * DEFAULT_NODE_INTERVAL_POS);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }


    public void deleteTestPlanResource(@Validated TestPlanResourceAssociationParam associationParam) {
        TestPlanFunctionalCaseExample testPlanFunctionalCaseExample = new TestPlanFunctionalCaseExample();
        testPlanFunctionalCaseExample.createCriteria().andIdIn(associationParam.getResourceIdList());
        testPlanFunctionalCaseMapper.deleteByExample(testPlanFunctionalCaseExample);
        extTestPlanCaseExecuteHistoryMapper.updateDeleted(associationParam.getResourceIdList(), true);
    }

    public TestPlanOperationResponse sortNode(ResourceSortRequest request, LogInsertModule logInsertModule) {
        TestPlanFunctionalCase dragNode = testPlanFunctionalCaseMapper.selectByPrimaryKey(request.getMoveId());
        if (dragNode == null) {
            throw new MSException(Translator.get("test_plan.drag.node.error"));
        }
        TestPlanOperationResponse response = new TestPlanOperationResponse();
        MoveNodeSortDTO sortDTO = super.getNodeSortDTO(
                request.getTestCollectionId(),
                super.getNodeMoveRequest(request, false),
                extTestPlanFunctionalCaseMapper::selectDragInfoById,
                extTestPlanFunctionalCaseMapper::selectNodeByPosOperator
        );
        super.sort(sortDTO);
        response.setOperationCount(1);
        TestPlan testPlan = testPlanMapper.selectByPrimaryKey(dragNode.getTestPlanId());
        testPlanResourceLogService.saveSortLog(testPlan, request.getMoveId(), new ResourceLogInsertModule(TestPlanResourceConstants.RESOURCE_FUNCTIONAL_CASE, logInsertModule));
        return response;
    }


    public List<TestPlanCasePageResponse> getFunctionalCasePage(TestPlanCaseRequest request, boolean deleted, String projectId) {
        request.setNullExecutorKey(filterCaseRequest(request.getFilter()));
        List<TestPlanCasePageResponse> functionalCaseLists = extTestPlanFunctionalCaseMapper.getCasePage(request, deleted, request.getSortString());
        if (CollectionUtils.isEmpty(functionalCaseLists)) {
            return new ArrayList<>();
        }
        //处理自定义字段值
        return handleCustomFields(functionalCaseLists, projectId);
    }

    private List<TestPlanCasePageResponse> handleCustomFields(List<TestPlanCasePageResponse> functionalCaseLists, String projectId) {
        List<String> ids = functionalCaseLists.stream().map(TestPlanCasePageResponse::getCaseId).collect(Collectors.toList());
        Map<String, List<FunctionalCaseCustomFieldDTO>> collect = functionalCaseService.getCaseCustomFiledMap(ids, projectId);
        Set<String> userIds = extractUserIds(functionalCaseLists);
        List<String> associateIds = functionalCaseLists.stream().map(TestPlanCasePageResponse::getId).toList();
        Map<String, List<TestPlanCaseBugDTO>> associateBugMap = queryCaseAssociateBug(associateIds, projectId);
        Map<String, String> userMap = userLoginService.getUserNameMap(new ArrayList<>(userIds));
        List<String> moduleIds = functionalCaseLists.stream().map(TestPlanCasePageResponse::getModuleId).toList();
        List<FunctionalCaseModule> modules = extFunctionalCaseModuleMapper.getNameInfoByIds(moduleIds);
        Map<String, String> moduleNameMap = modules.stream().collect(Collectors.toMap(FunctionalCaseModule::getId, FunctionalCaseModule::getName));
        functionalCaseLists.forEach(testPlanCasePageResponse -> {
            testPlanCasePageResponse.setCustomFields(collect.get(testPlanCasePageResponse.getCaseId()));
            testPlanCasePageResponse.setCreateUserName(userMap.get(testPlanCasePageResponse.getCreateUser()));
            testPlanCasePageResponse.setExecuteUserName(userMap.get(testPlanCasePageResponse.getExecuteUser()));
            testPlanCasePageResponse.setModuleName(StringUtils.isNotBlank(moduleNameMap.get(testPlanCasePageResponse.getModuleId())) ? moduleNameMap.get(testPlanCasePageResponse.getModuleId()) : Translator.get("functional_case.module.default.name"));
            if (associateBugMap.containsKey(testPlanCasePageResponse.getId())) {
                List<TestPlanCaseBugDTO> associateBugs = associateBugMap.get(testPlanCasePageResponse.getId());
                testPlanCasePageResponse.setBugList(associateBugs);
                testPlanCasePageResponse.setBugCount(associateBugs.size());
            }
        });
        return functionalCaseLists;

    }

    private List<CaseRelateBugDTO> handleStatus(List<CaseRelateBugDTO> bugDTOList, Map<String, String> statusMap) {
        bugDTOList.forEach(bugDTO -> {
            bugDTO.setStatus(statusMap.get(bugDTO.getStatus()));
        });
        return bugDTOList;
    }


    public Set<String> extractUserIds(List<TestPlanCasePageResponse> list) {
        return list.stream()
                .flatMap(testPlanCasePageResponse -> Stream.of(testPlanCasePageResponse.getUpdateUser(), testPlanCasePageResponse.getCreateUser(), testPlanCasePageResponse.getExecuteUser()))
                .collect(Collectors.toSet());
    }

    public List<BaseTreeNode> getTree(TestPlanTreeRequest request) {
        switch (request.getTreeType()) {
            case TreeTypeEnums.MODULE:
                return getModuleTree(request.getTestPlanId());
            case TreeTypeEnums.COLLECTION:
                return getCollectionTree(request.getTestPlanId());
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 已关联功能用例规划视图树
     *
     * @param testPlanId
     * @return
     */
    private List<BaseTreeNode> getCollectionTree(String testPlanId) {
        List<BaseTreeNode> returnList = new ArrayList<>();
        TestPlanCollectionExample collectionExample = new TestPlanCollectionExample();
        collectionExample.createCriteria().andTypeEqualTo(CaseType.FUNCTIONAL_CASE.getKey()).andParentIdNotEqualTo(ModuleConstants.ROOT_NODE_PARENT_ID).andTestPlanIdEqualTo(testPlanId);
        collectionExample.setOrderByClause("pos asc");
        List<TestPlanCollection> testPlanCollections = testPlanCollectionMapper.selectByExample(collectionExample);
        testPlanCollections.forEach(item -> {
            BaseTreeNode baseTreeNode = new BaseTreeNode(item.getId(), Translator.get(item.getName(), item.getName()), CaseType.FUNCTIONAL_CASE.getKey());
            returnList.add(baseTreeNode);
        });
        return returnList;
    }

    /**
     * 模块树
     *
     * @param testPlanId
     * @return
     */
    private List<BaseTreeNode> getModuleTree(String testPlanId) {
        List<BaseTreeNode> returnList = new ArrayList<>();
        List<ProjectOptionDTO> moduleLists = extTestPlanFunctionalCaseMapper.selectRootIdByTestPlanId(testPlanId);
        // 获取所有的项目id
        List<String> projectIds = moduleLists.stream().map(ProjectOptionDTO::getName).distinct().toList();
        // moduleLists中id=root的数据
        List<ProjectOptionDTO> rootModuleList = moduleLists.stream().filter(item -> StringUtils.equals(item.getId(), ModuleConstants.DEFAULT_NODE_ID)).toList();

        Map<String, List<ProjectOptionDTO>> projectRootMap = rootModuleList.stream().collect(Collectors.groupingBy(ProjectOptionDTO::getName));
        List<FunctionalCaseModuleDTO> functionalModuleIds = extTestPlanFunctionalCaseMapper.selectBaseByProjectIdAndTestPlanId(testPlanId);
        Map<String, List<FunctionalCaseModuleDTO>> projectModuleMap = functionalModuleIds.stream().collect(Collectors.groupingBy(FunctionalCaseModule::getProjectId));

        projectIds.forEach(projectId -> {
            // 如果projectRootMap中没有projectId，说明该项目没有根节点 不需要创
            // projectModuleMap中没有projectId，说明该项目没有模块 不需要创建
            // 如果都有 需要创建完整的数结构
            boolean needCreatRoot = MapUtils.isNotEmpty(projectRootMap) && projectRootMap.containsKey(projectId);
            boolean needCreatModule = MapUtils.isNotEmpty(projectModuleMap) && projectModuleMap.containsKey(projectId);
            // 项目名称是
            String projectName = needCreatModule ? projectModuleMap.get(projectId).getFirst().getProjectName() : projectRootMap.get(projectId).getFirst().getProjectName();
            // 构建项目那一层级
            BaseTreeNode projectNode = new BaseTreeNode(projectId, projectName, "PROJECT");
            returnList.add(projectNode);
            List<BaseTreeNode> nodeByNodeIds = new ArrayList<>();
            if (needCreatModule) {
                List<String> projectModuleIds = projectModuleMap.get(projectId).stream().map(FunctionalCaseModuleDTO::getId).toList();
                nodeByNodeIds = functionalCaseModuleService.getNodeByNodeIds(projectModuleIds);
            }
            List<BaseTreeNode> baseTreeNodes = functionalCaseModuleService.buildTreeAndCountResource(nodeByNodeIds, needCreatRoot, Translator.get("functional_case.module.default.name"));
            for (BaseTreeNode baseTreeNode : baseTreeNodes) {
                if (StringUtils.equals(baseTreeNode.getId(), ModuleConstants.DEFAULT_NODE_ID)) {
                    // 默认拼项目id
                    baseTreeNode.setId(projectId + "_" + ModuleConstants.DEFAULT_NODE_ID);
                }
                projectNode.addChild(baseTreeNode);
            }
        });
        return returnList;
    }


    public Map<String, Long> moduleCount(TestPlanCaseModuleRequest request) {
        request.setNullExecutorKey(filterCaseRequest(request.getFilter()));
        switch (request.getTreeType()) {
            case TreeTypeEnums.MODULE:
                return getModuleCount(request);
            case TreeTypeEnums.COLLECTION:
                return getCollectionCount(request);
            default:
                return new HashMap<>();
        }
    }

    /**
     * 已关联接口用例规划视图统计
     *
     * @param request
     * @return
     */
    private Map<String, Long> getCollectionCount(TestPlanCaseModuleRequest request) {
        request.setCollectionId(null);
        Map<String, Long> projectModuleCountMap = new HashMap<>();
        List<ModuleCountDTO> list = extTestPlanFunctionalCaseMapper.collectionCountByRequest(request);
        list.forEach(item -> {
            projectModuleCountMap.put(item.getModuleId(), (long) item.getDataCount());
        });
        long allCount = extTestPlanFunctionalCaseMapper.caseCount(request, false);
        projectModuleCountMap.put(CASE_MODULE_COUNT_ALL, allCount);
        return projectModuleCountMap;
    }

    /**
     * 已关联接口用例模块树统计
     *
     * @param request
     * @return
     */
    private Map<String, Long> getModuleCount(TestPlanCaseModuleRequest request) {
        //查出每个模块节点下的资源数量。 不需要按照模块进行筛选
        request.setModuleIds(null);
        List<FunctionalCaseModuleCountDTO> projectModuleCountDTOList = extTestPlanFunctionalCaseMapper.countModuleIdByRequest(request, false);
        Map<String, List<FunctionalCaseModuleCountDTO>> projectCountMap = projectModuleCountDTOList.stream().collect(Collectors.groupingBy(FunctionalCaseModuleCountDTO::getProjectId));
        Map<String, Long> projectModuleCountMap = projectModuleCountDTOList.stream()
                .filter(item -> StringUtils.equals(item.getModuleId(), item.getProjectId() + "_" + ModuleConstants.DEFAULT_NODE_ID))
                .collect(Collectors.groupingBy(FunctionalCaseModuleCountDTO::getModuleId, Collectors.summingLong(FunctionalCaseModuleCountDTO::getDataCount)));
        projectCountMap.forEach((projectId, moduleCountDTOList) -> {
            List<ModuleCountDTO> moduleCountDTOS = new ArrayList<>();
            for (FunctionalCaseModuleCountDTO functionalCaseModuleCountDTO : moduleCountDTOList) {
                ModuleCountDTO moduleCountDTO = new ModuleCountDTO();
                BeanUtils.copyBean(moduleCountDTO, functionalCaseModuleCountDTO);
                moduleCountDTOS.add(moduleCountDTO);
            }
            int sum = moduleCountDTOList.stream().mapToInt(FunctionalCaseModuleCountDTO::getDataCount).sum();
            Map<String, Long> moduleCountMap = getModuleCountMap(projectId, request.getTestPlanId(), moduleCountDTOS);
            moduleCountMap.forEach((k, v) -> {
                if (projectModuleCountMap.get(k) == null || projectModuleCountMap.get(k) == 0L) {
                    projectModuleCountMap.put(k, v);
                }
            });
            projectModuleCountMap.put(projectId, (long) sum);
        });
        //查出全部用例数量
        long allCount = extTestPlanFunctionalCaseMapper.caseCount(request, false);
        projectModuleCountMap.put(CASE_MODULE_COUNT_ALL, allCount);
        return projectModuleCountMap;
    }


    public Map<String, Long> getModuleCountMap(String projectId, String testPlanId, List<ModuleCountDTO> moduleCountDTOList) {
        //构建模块树，并计算每个节点下的所有数量（包含子节点）
        List<BaseTreeNode> treeNodeList = this.getTreeOnlyIdsAndResourceCount(projectId, testPlanId, moduleCountDTOList);

        //通过广度遍历的方式构建返回值
        return functionalCaseModuleService.getIdCountMapByBreadth(treeNodeList);
    }

    public List<BaseTreeNode> getTreeOnlyIdsAndResourceCount(String projectId, String testPlanId, List<ModuleCountDTO> moduleCountDTOList) {
        //节点内容只有Id和parentId
        List<String> moduleIds = extTestPlanModuleMapper.selectIdByProjectIdAndTestPlanId(projectId, testPlanId);
        List<BaseTreeNode> nodeByNodeIds = functionalCaseModuleService.getNodeByNodeIds(moduleIds);
        return functionalCaseModuleService.buildTreeAndCountResource(nodeByNodeIds, moduleCountDTOList, true, Translator.get("functional_case.module.default.name"));


    }

    public TestPlanAssociationResponse disassociate(BasePlanCaseBatchRequest request, LogInsertModule logInsertModule) {
        List<String> selectIds = doSelectIds(request);
        return super.disassociate(
                TestPlanResourceConstants.RESOURCE_FUNCTIONAL_CASE,
                request,
                logInsertModule,
                selectIds,
                this::deleteTestPlanResource);
    }

    public List<String> doSelectIds(BasePlanCaseBatchRequest request) {
        if (request.isSelectAll()) {
            request.setNullExecutorKey(filterCaseRequest(request.getCondition().getFilter()));
            List<String> ids = extTestPlanFunctionalCaseMapper.getIds(request, false);
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                ids.removeAll(request.getExcludeIds());
            }
            return ids;
        } else {
            return request.getSelectIds();
        }
    }

    public void associateBug(TestPlanCaseAssociateBugRequest request, String userId) {
        super.associateBug(request, userId, CaseType.FUNCTIONAL_CASE.getKey());
    }

    /**
     * 执行功能用例
     *
     * @param request
     * @param logInsertModule
     */
    public void run(TestPlanCaseRunRequest request, LogInsertModule logInsertModule) {
        TestPlanFunctionalCase functionalCase = new TestPlanFunctionalCase();
        functionalCase.setLastExecResult(request.getLastExecResult());
        functionalCase.setLastExecTime(System.currentTimeMillis());
        functionalCase.setExecuteUser(logInsertModule.getOperator());
        functionalCase.setId(request.getId());
        testPlanFunctionalCaseMapper.updateByPrimaryKeySelective(functionalCase);

        //更新用例表执行状态
        updateFunctionalCaseStatus(Collections.singletonList(request.getCaseId()), request.getLastExecResult());

        //执行记录
        TestPlanCaseExecuteHistory executeHistory = buildHistory(request, logInsertModule.getOperator());
        handleFileAndNotice(request.getCaseId(), request.getProjectId(), request.getPlanCommentFileIds(), logInsertModule.getOperator(), CaseFileSourceType.PLAN_COMMENT.toString(), request.getNotifier(), request.getTestPlanId(), request.getLastExecResult());
        testPlanCaseExecuteHistoryMapper.insert(executeHistory);

        // 写入执行数据到 test_plan_case_metrics 表（包含ExecutionTracker埋点数据和基本执行信息）
        try {
            saveExecutionTimeMetrics(request, logInsertModule.getOperator());
        } catch (Exception e) {
            // 执行数据写入失败不影响主流程，只记录日志
            io.vanguard.testops.sdk.util.LogUtils.error("保存执行数据失败，用例ID: " + request.getCaseId(), e);
        }

        // 写入用例执行记录到 case_execution_record 表（用于CS指标统计）
        try {
            saveCaseExecutionRecord(request, logInsertModule.getOperator());
        } catch (Exception e) {
            // 执行记录写入失败不影响主流程，只记录日志
            io.vanguard.testops.sdk.util.LogUtils.error("保存用例执行记录失败，用例ID: " + request.getCaseId(), e);
        }

    }

    /**
     * 更新功能用例表的执行状态
     *
     * @param ids
     * @param lastExecResult
     */
    private void updateFunctionalCaseStatus(List<String> ids, String lastExecResult) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        FunctionalCaseMapper functionalCaseMapper = sqlSession.getMapper(FunctionalCaseMapper.class);
        ids.forEach(id -> {
            FunctionalCase functionalCase = new FunctionalCase();
            functionalCase.setId(id);
            functionalCase.setLastExecuteResult(lastExecResult);
            functionalCaseMapper.updateByPrimaryKeySelective(functionalCase);
        });
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }


    private TestPlanCaseExecuteHistory buildHistory(TestPlanCaseRunRequest request, String operator) {
        TestPlanCaseExecuteHistory executeHistory = new TestPlanCaseExecuteHistory();
        executeHistory.setId(IDGenerator.nextStr());
        executeHistory.setTestPlanCaseId(request.getId());
        executeHistory.setTestPlanId(request.getTestPlanId());
        executeHistory.setCaseId(request.getCaseId());
        executeHistory.setStatus(request.getLastExecResult());
        if (StringUtils.isNotBlank(request.getContent())) {
            executeHistory.setContent(request.getContent().getBytes());
        }
        executeHistory.setSteps(StringUtils.defaultIfBlank(request.getStepsExecResult(), StringUtils.EMPTY).getBytes(StandardCharsets.UTF_8));
        executeHistory.setDeleted(false);
        executeHistory.setNotifier(request.getNotifier());
        executeHistory.setCreateUser(operator);
        executeHistory.setCreateTime(System.currentTimeMillis());
        return executeHistory;
    }

    /**
     * 保存用例执行记录到 case_execution_record 表（用于CS指标统计）
     */
    private void saveCaseExecutionRecord(TestPlanCaseRunRequest request, String operator) {
        // 检查该用例在该测试计划中是否首次执行
        boolean isFirstExec = caseExecutionRecordMapper.countByCaseId(request.getCaseId()) == 0;
        
        // 获取用例的CS值
        java.math.BigDecimal caseCsScore = null;
        try {
            io.vanguard.testops.functional.domain.CaseMetricsDetail csDetail = caseCSCalculationService.getOrCalculateCSDetail(request.getCaseId());
            if (csDetail != null && csDetail.getCsScore() != null) {
                caseCsScore = csDetail.getCsScore();
            }
        } catch (Exception e) {
            io.vanguard.testops.sdk.util.LogUtils.warn("获取用例CS值失败，用例ID: " + request.getCaseId(), e);
        }
        
        // 构建执行记录
        io.vanguard.testops.functional.domain.CaseExecutionRecord record = new io.vanguard.testops.functional.domain.CaseExecutionRecord();
        record.setId(IDGenerator.nextStr());
        record.setCaseId(request.getCaseId());
        record.setPlanId(request.getTestPlanId());
        record.setProjectId(request.getProjectId());
        record.setExecutorId(operator);
        record.setStatus(request.getLastExecResult());
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis());
        record.setDuration(request.getActualExecMs() != null ? request.getActualExecMs() : 0L);
        record.setIsFirstExecution(isFirstExec ? 1 : 0);
        record.setCaseCsScore(caseCsScore);
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        
        // 保存到数据库
        caseExecutionRecordMapper.insert(record);
    }

    /**
     * 保存执行数据到 test_plan_case_metrics 表（包含ExecutionTracker埋点数据和基本执行信息）
     */
    private void saveExecutionTimeMetrics(TestPlanCaseRunRequest request, String operator) {
        long currentTime = System.currentTimeMillis();
        
        // 查询是否已存在记录
        io.vanguard.testops.plan.domain.TestPlanCaseMetrics metrics = testPlanCaseMetricsMapper.selectByPlanAndCase(
            request.getTestPlanId(), request.getCaseId());
        
        if (metrics != null) {
            // 更新现有记录
            
            // 增加执行次数
            metrics.setExecCount((metrics.getExecCount() == null ? 0 : metrics.getExecCount()) + 1);
            
            // 更新最终执行结果
            metrics.setLastExecResult(request.getLastExecResult());
            
            // 累加执行耗时字段（多次执行累加）
            if (request.getActualExecMs() != null) {
                long currentExecMs = metrics.getActualExecMs() == null ? 0L : metrics.getActualExecMs();
                metrics.setActualExecMs(currentExecMs + request.getActualExecMs());
            }
            if (request.getActualReadingMs() != null) {
                long currentReadingMs = metrics.getActualReadingMs() == null ? 0L : metrics.getActualReadingMs();
                metrics.setActualReadingMs(currentReadingMs + request.getActualReadingMs());
            }
            if (request.getIsBatchFill() != null) {
                // 是否批量填表：保留最新的状态
                metrics.setIsBatchFill(request.getIsBatchFill() ? 1 : 0);
            }
            if (request.getFocusOutCount() != null) {
                int currentFocusOutCount = metrics.getFocusOutCount() == null ? 0 : metrics.getFocusOutCount();
                metrics.setFocusOutCount(currentFocusOutCount + request.getFocusOutCount());
            }
            if (request.getFilteredTimeMs() != null) {
                long currentFilteredTime = metrics.getFilteredTimeMs() == null ? 0L : metrics.getFilteredTimeMs();
                metrics.setFilteredTimeMs(currentFilteredTime + request.getFilteredTimeMs());
            }
            
            // 更新阻塞状态和原因
            if (Boolean.TRUE.equals(request.getIsBlocked())) {
                metrics.setIsBlockedRun(true);
                if (StringUtils.isNotBlank(request.getBlockReason())) {
                    metrics.setBlockReason(request.getBlockReason());
                }
            } else {
                metrics.setIsBlockedRun(false);
                metrics.setBlockReason(null);
            }
            
            // 自动计算总耗时
            long totalTime = (metrics.getActualExecMs() == null ? 0L : metrics.getActualExecMs()) +
                             (metrics.getActualReadingMs() == null ? 0L : metrics.getActualReadingMs());
            metrics.setTotalTimeMs(totalTime);
            
            metrics.setUpdateTime(currentTime);
            
            testPlanCaseMetricsMapper.updateByPrimaryKey(metrics);
            
            // 检查是否因环境因素阻塞，需要调整环境因子
            checkAndAdjustEnvFactor(request);
        } else {
            // 创建新记录
            metrics = new io.vanguard.testops.plan.domain.TestPlanCaseMetrics();
            metrics.setId(IDGenerator.nextStr());
            metrics.setTestPlanId(request.getTestPlanId());
            metrics.setCaseId(request.getCaseId());
            metrics.setProjectId(request.getProjectId());
            
            // 设置执行耗时字段
            metrics.setActualExecMs(request.getActualExecMs() != null ? request.getActualExecMs() : 0L);
            metrics.setActualReadingMs(request.getActualReadingMs() != null ? request.getActualReadingMs() : 0L);
            metrics.setIsBatchFill(request.getIsBatchFill() != null && request.getIsBatchFill() ? 1 : 0);
            metrics.setFocusOutCount(request.getFocusOutCount() != null ? request.getFocusOutCount() : 0);
            metrics.setFilteredTimeMs(request.getFilteredTimeMs() != null ? request.getFilteredTimeMs() : 0L);
            
            // 设置阻塞状态和原因
            if (Boolean.TRUE.equals(request.getIsBlocked())) {
                metrics.setIsBlockedRun(true);
                metrics.setBlockReason(request.getBlockReason());
            } else {
                metrics.setIsBlockedRun(false);
                metrics.setBlockReason(null);
            }
            
            // 设置其他必填字段的默认值
            metrics.setCaseSourceType("NEW");
            metrics.setExecCount(1);
            metrics.setFirstExecTime(currentTime);
            metrics.setFirstExecResult(request.getLastExecResult());
            metrics.setLastExecResult(request.getLastExecResult());
            
            // 自动计算总耗时
            long totalTime = (metrics.getActualExecMs() == null ? 0L : metrics.getActualExecMs()) +
                             (metrics.getActualReadingMs() == null ? 0L : metrics.getActualReadingMs());
            metrics.setTotalTimeMs(totalTime);
            
            metrics.setCreateTime(currentTime);
            metrics.setUpdateTime(currentTime);
            
            testPlanCaseMetricsMapper.insert(metrics);
            
            // 检查是否因环境因素阻塞，需要调整环境因子
            checkAndAdjustEnvFactor(request);
        }
    }
    
    /**
     * 检查并调整环境因子
     * 如果用例因环境因素阻塞，将环境因子从 1.0 调整为 1.5，并重新计算理论工时
     * 
     * @param request 执行请求
     */
    private void checkAndAdjustEnvFactor(TestPlanCaseRunRequest request) {
        try {
            // 只有在环境因素阻塞时才调整
            if (!"ENVIRONMENT".equals(request.getBlockReason())) {
                return;
            }
            
            // 查询用例的指标详情
            io.vanguard.testops.functional.domain.CaseMetricsDetail csDetail = 
                caseMetricsDetailMapper.selectByCaseId(request.getCaseId());
            
            if (csDetail == null) {
                io.vanguard.testops.sdk.util.LogUtils.warn("未找到用例指标详情，无法调整环境因子: caseId={}", request.getCaseId());
                return;
            }
            
            // 检查环境因子是否已经是 1.5
            if (csDetail.getEnvFactor() != null && 
                csDetail.getEnvFactor().compareTo(BigDecimal.valueOf(1.5)) == 0) {
                io.vanguard.testops.sdk.util.LogUtils.info("环境因子已经是1.5，无需调整: caseId={}", request.getCaseId());
                return;
            }
            
            // 调整环境因子为 1.5
            BigDecimal oldEnvFactor = csDetail.getEnvFactor() != null ? csDetail.getEnvFactor() : BigDecimal.ONE;
            BigDecimal newEnvFactor = BigDecimal.valueOf(1.5);
            
            // 重新计算理论工时（应用新的环境因子）
            // 新工时 = 当前工时 / 旧因子 * 新因子
            long oldWriteMs = csDetail.getAlgoExpectedWriteMs() != null ? csDetail.getAlgoExpectedWriteMs() : 0L;
            long oldExecMs = csDetail.getAlgoExpectedExecMs() != null ? csDetail.getAlgoExpectedExecMs() : 0L;
            
            long newWriteMs = (long) (oldWriteMs / oldEnvFactor.doubleValue() * newEnvFactor.doubleValue());
            long newExecMs = (long) (oldExecMs / oldEnvFactor.doubleValue() * newEnvFactor.doubleValue());
            
            csDetail.setEnvFactor(newEnvFactor);
            csDetail.setAlgoExpectedWriteMs(newWriteMs);
            csDetail.setAlgoExpectedExecMs(newExecMs);
            
            // 如果是复用用例，同步更新节约工时
            if ("REUSE".equals(csDetail.getCaseSourceType()) && 
                csDetail.getModificationCostMs() != null && csDetail.getModificationCostMs() > 0) {
                long savedMs = newWriteMs - csDetail.getModificationCostMs();
                csDetail.setSavedWriteMs(Math.max(0, savedMs));
            }
            
            csDetail.setUpdateTime(System.currentTimeMillis());
            caseMetricsDetailMapper.updateByPrimaryKey(csDetail);
            
            io.vanguard.testops.sdk.util.LogUtils.info("因环境阻塞调整环境因子: caseId={}, oldEnvFactor={}, newEnvFactor={}, " +
                    "oldWriteMs={}ms, newWriteMs={}ms, oldExecMs={}ms, newExecMs={}ms", 
                request.getCaseId(), oldEnvFactor, newEnvFactor, 
                oldWriteMs, newWriteMs, oldExecMs, newExecMs);
            
        } catch (Exception e) {
            io.vanguard.testops.sdk.util.LogUtils.error("调整环境因子失败: caseId=" + request.getCaseId(), e);
        }
    }

    /**
     * 批量保存用例执行记录到 case_execution_record 表（用于CS指标统计）
     */
    private void saveBatchCaseExecutionRecords(List<String> caseIds, TestPlanCaseBatchRunRequest request, 
                                               LogInsertModule logInsertModule, Map<String, String> projectMap) {
        List<io.vanguard.testops.functional.domain.CaseExecutionRecord> records = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (String caseId : caseIds) {
            // 检查该用例是否首次执行
            boolean isFirstExec = caseExecutionRecordMapper.countByCaseId(caseId) == 0;
            
            // 获取用例的CS值
            java.math.BigDecimal caseCsScore = null;
            try {
                io.vanguard.testops.functional.domain.CaseMetricsDetail csDetail = caseCSCalculationService.getOrCalculateCSDetail(caseId);
                if (csDetail != null && csDetail.getCsScore() != null) {
                    caseCsScore = csDetail.getCsScore();
                }
            } catch (Exception e) {
                io.vanguard.testops.sdk.util.LogUtils.warn("获取用例CS值失败，用例ID: " + caseId, e);
            }
            
            // 构建执行记录
            io.vanguard.testops.functional.domain.CaseExecutionRecord record = new io.vanguard.testops.functional.domain.CaseExecutionRecord();
            record.setId(IDGenerator.nextStr());
            record.setCaseId(caseId);
            record.setPlanId(request.getTestPlanId());
            record.setProjectId(projectMap.get(caseId));
            record.setExecutorId(logInsertModule.getOperator());
            record.setStatus(request.getLastExecResult());
            record.setStartTime(currentTime);
            record.setEndTime(currentTime);
            record.setDuration(0L);
            record.setIsFirstExecution(isFirstExec ? 1 : 0);
            record.setCaseCsScore(caseCsScore);
            record.setCreateTime(currentTime);
            record.setUpdateTime(currentTime);
            
            records.add(record);
        }
        
        // 批量保存到数据库
        if (CollectionUtils.isNotEmpty(records)) {
            caseExecutionRecordMapper.batchInsert(records);
        }
    }

    /**
     * 批量保存执行数据到 test_plan_case_metrics 表
     * 批量执行不追踪耗时，只更新执行次数和结果
     */
    private void saveBatchExecutionMetrics(List<String> planCaseIds, List<String> caseIds, 
                                           TestPlanCaseBatchRunRequest request, Map<String, String> projectMap) {
        long currentTime = System.currentTimeMillis();
        
        for (String caseId : caseIds) {
            // 查询是否已存在记录
            io.vanguard.testops.plan.domain.TestPlanCaseMetrics metrics = testPlanCaseMetricsMapper.selectByPlanAndCase(
                request.getTestPlanId(), caseId);
            
            if (metrics != null) {
                // 更新现有记录
                metrics.setExecCount((metrics.getExecCount() == null ? 0 : metrics.getExecCount()) + 1);
                metrics.setLastExecResult(request.getLastExecResult());
                
                // 更新阻塞状态（批量执行时根据执行结果判断）
                if ("BLOCKED".equals(request.getLastExecResult())) {
                    metrics.setIsBlockedRun(true);
                }
                
                // 标记为批量填表
                metrics.setIsBatchFill(1);
                
                // 批量提交：不更新时间字段，等待后续批量分配
                // total_time_ms 将在 redistributeBatchExecutionTime 中统一分配
                
                metrics.setUpdateTime(currentTime);
                testPlanCaseMetricsMapper.updateByPrimaryKey(metrics);
                
                // 触发当天批量用例时间重新分配
                redistributeBatchExecutionTime(request.getTestPlanId(), currentTime);
            } else {
                // 创建新记录
                metrics = new io.vanguard.testops.plan.domain.TestPlanCaseMetrics();
                metrics.setId(IDGenerator.nextStr());
                metrics.setTestPlanId(request.getTestPlanId());
                metrics.setCaseId(caseId);
                metrics.setProjectId(projectMap.get(caseId));
                metrics.setCaseSourceType("NEW");
                metrics.setExecCount(1);
                metrics.setFirstExecTime(currentTime);
                metrics.setFirstExecResult(request.getLastExecResult());
                metrics.setLastExecResult(request.getLastExecResult());
                metrics.setIsBlockedRun("BLOCKED".equals(request.getLastExecResult()));
                
                // 批量执行不追踪耗时，设为0
                metrics.setActualExecMs(0L);
                metrics.setActualReadingMs(0L);
                metrics.setIsBatchFill(1); // 标记为批量填表
                metrics.setFocusOutCount(0);
                metrics.setFilteredTimeMs(0L);
                metrics.setTotalTimeMs(0L); // 初始为0，等待后续批量分配
                
                metrics.setCreateTime(currentTime);
                metrics.setUpdateTime(currentTime);
                
                testPlanCaseMetricsMapper.insert(metrics);
                
                // 触发当天批量用例时间重新分配
                redistributeBatchExecutionTime(request.getTestPlanId(), currentTime);
            }
        }
    }

    public List<BugProviderDTO> hasAssociateBugPage(AssociateBugPageRequest request) {
        return baseAssociateBugProvider.hasTestPlanAssociateBugPage(request);
    }


    /**
     * 批量执行功能用例
     *
     * @param request
     * @param logInsertModule
     */
    public void batchRun(TestPlanCaseBatchRunRequest request, LogInsertModule logInsertModule) {
        List<String> ids = doSelectIds(request);
        if (CollectionUtils.isNotEmpty(ids)) {
            handleBatchRun(ids, request, logInsertModule);
        }

    }

    private void handleBatchRun(List<String> ids, TestPlanCaseBatchRunRequest request, LogInsertModule logInsertModule) {
        //更新状态
        extTestPlanFunctionalCaseMapper.batchUpdate(ids, request.getLastExecResult(), System.currentTimeMillis(), logInsertModule.getOperator());

        //执行记录
        TestPlanFunctionalCaseExample example = new TestPlanFunctionalCaseExample();
        example.createCriteria().andIdIn(ids);
        List<TestPlanFunctionalCase> functionalCases = testPlanFunctionalCaseMapper.selectByExample(example);
        List<String> caseIds = functionalCases.stream().map(TestPlanFunctionalCase::getFunctionalCaseId).collect(Collectors.toList());
        Map<String, String> idsMap = functionalCases.stream().collect(Collectors.toMap(TestPlanFunctionalCase::getId, TestPlanFunctionalCase::getFunctionalCaseId));
        List<FunctionalCase> list = extFunctionalCaseMapper.getProjectIdByIds(caseIds);
        Map<String, String> projectMap = list.stream().collect(Collectors.toMap(FunctionalCase::getId, FunctionalCase::getProjectId));
        List<TestPlanCaseExecuteHistory> historyList = getExecHistory(ids, request, logInsertModule, idsMap, projectMap);
        testPlanCaseExecuteHistoryMapper.batchInsert(historyList);

        updateFunctionalCaseStatus(caseIds, request.getLastExecResult());

        // 批量写入用例执行记录到 case_execution_record 表（用于CS指标统计）
        try {
            saveBatchCaseExecutionRecords(caseIds, request, logInsertModule, projectMap);
        } catch (Exception e) {
            // 执行记录写入失败不影响主流程，只记录日志
            io.vanguard.testops.sdk.util.LogUtils.error("批量保存用例执行记录失败", e);
        }

        // 批量写入执行数据到 test_plan_case_metrics 表
        try {
            saveBatchExecutionMetrics(ids, caseIds, request, projectMap);
        } catch (Exception e) {
            // 执行数据写入失败不影响主流程，只记录日志
            io.vanguard.testops.sdk.util.LogUtils.error("批量保存执行数据到test_plan_case_metrics失败", e);
        }

    }

    private List<TestPlanCaseExecuteHistory> getExecHistory(List<String> ids, TestPlanCaseBatchRunRequest request, LogInsertModule logInsertModule, Map<String, String> idsMap, Map<String, String> projectMap) {

        List<TestPlanCaseExecuteHistory> historyList = new ArrayList<>();
        ids.forEach(id -> {
            TestPlanCaseExecuteHistory executeHistory = new TestPlanCaseExecuteHistory();
            executeHistory.setId(IDGenerator.nextStr());
            executeHistory.setTestPlanCaseId(id);
            executeHistory.setTestPlanId(request.getTestPlanId());
            executeHistory.setCaseId(idsMap.get(id));
            executeHistory.setStatus(request.getLastExecResult());
            executeHistory.setContent(request.getContent().getBytes());
            executeHistory.setDeleted(false);
            executeHistory.setNotifier(request.getNotifier());
            executeHistory.setCreateUser(logInsertModule.getOperator());
            executeHistory.setCreateTime(System.currentTimeMillis());
            historyList.add(executeHistory);
            String caseId = idsMap.get(id);
            handleFileAndNotice(caseId, projectMap.get(caseId), request.getPlanCommentFileIds(), logInsertModule.getOperator(), CaseFileSourceType.PLAN_COMMENT.toString(), request.getNotifier(), request.getTestPlanId(), request.getLastExecResult());


        });
        return historyList;
    }

    private void handleFileAndNotice(String caseId, String projectId, List<String> uploadFileIds, String userId, String fileSource, String notifier, String testPlanId, String lastExecResult) {
        //富文本评论的处理
        functionalCaseAttachmentService.uploadOssFile(caseId, projectId, uploadFileIds, userId, fileSource);

        //发通知
        if (StringUtils.isNotBlank(notifier)) {
            List<String> relatedUsers = Arrays.asList(notifier.split(";"));
            testPlanSendNoticeService.sendNoticeCase(relatedUsers, userId, caseId, NoticeConstants.TaskType.FUNCTIONAL_CASE_TASK, NoticeConstants.Event.EXECUTE_AT, testPlanId);
        }

        if (StringUtils.equalsIgnoreCase(lastExecResult, ResultStatus.SUCCESS.name())) {
            //成功 发送通知
            testPlanSendNoticeService.sendNoticeCase(new ArrayList<>(), userId, caseId, NoticeConstants.TaskType.FUNCTIONAL_CASE_TASK, NoticeConstants.Event.EXECUTE_PASSED, testPlanId);
        }

        if (StringUtils.equalsIgnoreCase(lastExecResult, ResultStatus.ERROR.name())) {
            //失败 发送通知
            testPlanSendNoticeService.sendNoticeCase(new ArrayList<>(), userId, caseId, NoticeConstants.TaskType.FUNCTIONAL_CASE_TASK, NoticeConstants.Event.EXECUTE_FAIL, testPlanId);
        }
    }


    /**
     * 批量更新执行人
     *
     * @param request
     */
    public void batchUpdateExecutor(TestPlanCaseUpdateRequest request) {
        List<String> ids = doSelectIds(request);
        if (CollectionUtils.isNotEmpty(ids)) {
            extTestPlanFunctionalCaseMapper.batchUpdateExecutor(ids, request.getUserId());
        }
    }

    public List<TestPlanCaseExecHistoryResponse> getCaseExecHistory(TestPlanCaseExecHistoryRequest request) {
        List<TestPlanCaseExecHistoryResponse> list = extTestPlanCaseExecuteHistoryMapper.getCaseExecHistory(request);
        list.forEach(item -> {
            if (item.getContent() != null) {
                item.setContentText(new String(item.getContent(), StandardCharsets.UTF_8));
            }
            if (item.getSteps() != null) {
                String historyStepStr = new String(item.getSteps(), StandardCharsets.UTF_8);
                item.setStepsExecResult(historyStepStr);
                if (StringUtils.isNotBlank(historyStepStr)) {
                    List<FunctionalCaseStepDTO> historySteps = JSON.parseArray(historyStepStr, FunctionalCaseStepDTO.class);
                    if (CollectionUtils.isNotEmpty(historySteps)) {
                        item.setShowResult(true);
                    }
                }
            }
        });
        return list;
    }

    public TestPlanCaseDetailResponse getFunctionalCaseDetail(String id, String userId) {
        TestPlanFunctionalCase planFunctionalCase = testPlanFunctionalCaseMapper.selectByPrimaryKey(id);
        if (planFunctionalCase == null) {
            throw new MSException(Translator.get("resource_not_exist"));
        }
        String caseId = planFunctionalCase.getFunctionalCaseId();
        FunctionalCaseDetailDTO functionalCaseDetail = functionalCaseService.getFunctionalCaseDetail(caseId, userId, false);
        String caseDetailSteps = functionalCaseDetail.getSteps();
        List<TestPlanCaseExecuteHistory> testPlanCaseExecuteHistories = extTestPlanCaseExecuteHistoryMapper.selectSteps(id, caseId);
        List<FunctionalCaseStepDTO> functionalCaseStepDTOS = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(testPlanCaseExecuteHistories)) {
            TestPlanCaseExecuteHistory testPlanCaseExecuteHistory = testPlanCaseExecuteHistories.getFirst();
            if (StringUtils.isNotBlank(caseDetailSteps)) {
                List<FunctionalCaseStepDTO> newCaseSteps = JSON.parseArray(caseDetailSteps, FunctionalCaseStepDTO.class);
                compareStep(testPlanCaseExecuteHistory, newCaseSteps);
                functionalCaseStepDTOS = newCaseSteps;
                functionalCaseDetail.setSteps(JSON.toJSONString(functionalCaseStepDTOS));
            }
            // 获取最新的备注内容
            if (testPlanCaseExecuteHistory.getContent() != null) {
                String content = new String(testPlanCaseExecuteHistory.getContent(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(content)) {
                    functionalCaseDetail.setDescription(content);
                }
            }
        } else {
            if (StringUtils.isNotBlank(caseDetailSteps)) {
                functionalCaseStepDTOS = JSON.parseArray(caseDetailSteps, FunctionalCaseStepDTO.class);
            }
            functionalCaseDetail.setSteps(JSON.toJSONString(functionalCaseStepDTOS));
        }
        TestPlanCaseDetailResponse response = new TestPlanCaseDetailResponse();
        BeanUtils.copyBean(response, functionalCaseDetail);
        response.setLastExecuteResult(planFunctionalCase.getLastExecResult());

        TestPlanCaseExecuteHistoryExample testPlanCaseExecuteHistoryExample = new TestPlanCaseExecuteHistoryExample();
        testPlanCaseExecuteHistoryExample.createCriteria().andCaseIdEqualTo(caseId).andTestPlanCaseIdEqualTo(id).andDeletedEqualTo(false);
        testPlanCaseExecuteHistoryExample.setOrderByClause("create_time DESC");
        response.setRunListCount((int) testPlanCaseExecuteHistoryMapper.countByExample(testPlanCaseExecuteHistoryExample));
        AssociateBugPageRequest associateBugPageRequest = new AssociateBugPageRequest();
        associateBugPageRequest.setTestPlanCaseId(id);
        associateBugPageRequest.setProjectId(functionalCaseDetail.getProjectId());
        List<BugProviderDTO> associateBugs = hasAssociateBugPage(associateBugPageRequest);
        response.setBugListCount(CollectionUtils.isNotEmpty(associateBugs) ? associateBugs.size() : 0);
        return response;
    }

    private Integer getBugListCount(String id, String testPlanId) {
        BugRelationCaseExample example = new BugRelationCaseExample();
        example.createCriteria().andTestPlanIdEqualTo(testPlanId).andTestPlanCaseIdEqualTo(id);
        return (int) bugRelationCaseMapper.countByExample(example);
    }

    private static void compareStep(TestPlanCaseExecuteHistory testPlanCaseExecuteHistory, List<FunctionalCaseStepDTO> newCaseSteps) {
        if (testPlanCaseExecuteHistory.getSteps() != null) {
            String historyStepStr = new String(testPlanCaseExecuteHistory.getSteps(), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(historyStepStr)) {
                List<FunctionalCaseStepDTO> historySteps = JSON.parseArray(historyStepStr, FunctionalCaseStepDTO.class);
                // 修复：如果 id 为 null，则使用 num 作为备用 key，避免 Duplicate key null 错误
                Map<String, FunctionalCaseStepDTO> historyStepMap = historySteps.stream().collect(Collectors.toMap(
                    step -> step.getId() != null ? step.getId() : String.valueOf(step.getNum()),
                    t -> t
                ));
                newCaseSteps.forEach(newCaseStep -> {
                    setHistoryInfo(newCaseStep, historyStepMap);
                });
            }
        }
    }

    private static void setHistoryInfo(FunctionalCaseStepDTO newCaseStep, Map<String, FunctionalCaseStepDTO> historyStepMap) {
        // 修复：使用 id 或 num 作为 key 来查找历史步骤，与 compareStep 中的逻辑保持一致
        String key = newCaseStep.getId() != null ? newCaseStep.getId() : String.valueOf(newCaseStep.getNum());
        FunctionalCaseStepDTO historyStep = historyStepMap.get(key);
        if (historyStep != null && StringUtils.equals(historyStep.getDesc(), newCaseStep.getDesc()) && StringUtils.equals(historyStep.getResult(), newCaseStep.getResult())) {
            newCaseStep.setExecuteResult(historyStep.getExecuteResult());
            newCaseStep.setActualResult(historyStep.getActualResult());
        }
    }

    /**
     * 获取项目下的所有用户
     *
     * @param projectId
     * @param keyword
     * @return
     */
    public List<UserDTO> getExecUserList(String projectId, String keyword) {
        return extUserMapper.getUserByKeyword(projectId, keyword);
    }

    @Override
    public void associateCollection(String planId, Map<String, List<BaseCollectionAssociateRequest>> collectionAssociates, SessionUser user) {
        List<TestPlanFunctionalCase> testPlanFunctionalCaseList = new ArrayList<>();
        List<BaseCollectionAssociateRequest> functionalList = collectionAssociates.get(AssociateCaseType.FUNCTIONAL);
        if (CollectionUtils.isNotEmpty(functionalList)) {
            TestPlan testPlan = testPlanMapper.selectByPrimaryKey(planId);
            boolean isRepeat = testPlanConfigService.isRepeatCase(testPlan.getId());
            functionalList.forEach(functional -> buildTestPlanFunctionalCase(testPlan, functional, user, testPlanFunctionalCaseList, isRepeat));
        }
        if (CollectionUtils.isNotEmpty(testPlanFunctionalCaseList)) {
            testPlanFunctionalCaseMapper.batchInsert(testPlanFunctionalCaseList);
            // Create metrics for each associated case
            testPlanFunctionalCaseList.forEach(this::createTestPlanCaseMetrics);
        }
    }

    private void createTestPlanCaseMetrics(TestPlanFunctionalCase testPlanCase) {
        try {
            String functionalCaseId = testPlanCase.getFunctionalCaseId();
            String testPlanId = testPlanCase.getTestPlanId();
            String projectId = null; 
            TestPlan testPlan = testPlanMapper.selectByPrimaryKey(testPlanId);
            if (testPlan != null) {
                projectId = testPlan.getProjectId();
            }

            // 1. Get CS Detail
            io.vanguard.testops.functional.domain.CaseMetricsDetail csDetail = caseCSCalculationService.getOrCalculateCSDetail(functionalCaseId);
            
            // 2. Determine Source Type
            String sourceType = determineCaseSourceType(functionalCaseId, testPlanId);
            
            // 3. Calculate Savings - 从 case_metrics_detail 读取修改耗时和节约工时
            long modificationCost = 0L;
            long savedWriteMs = 0L;
            
            if ("REUSE".equals(sourceType) || "MODIFY".equals(sourceType)) {
                // Update reuse count AND case_source_type
                if (csDetail != null) {
                    csDetail.setReuseCount((csDetail.getReuseCount() == null ? 0 : csDetail.getReuseCount()) + 1);
                    
                    // 关键修复：同步更新 case_metrics_detail 的 case_source_type
                    // 这样前端 getFunctionalCaseDetail 才能正确获取用例类型，启动 ModificationTracker
                    csDetail.setCaseSourceType(sourceType);
                    
                    caseMetricsDetailMapper.updateByPrimaryKey(csDetail);
                    
                    // 从 case_metrics_detail 读取修改耗时和节约工时（用例维度的数据）
                    modificationCost = csDetail.getModificationCostMs() != null ? csDetail.getModificationCostMs() : 0L;
                    savedWriteMs = csDetail.getSavedWriteMs() != null ? csDetail.getSavedWriteMs() : 0L;
                    
                    log.info("用例 {} 被复用到测试计划 {}, 类型: {}, 累计复用次数: {}", 
                        functionalCaseId, testPlanId, sourceType, csDetail.getReuseCount());
                }
            }

            // 4. Create Metrics Record - 快照修改耗时和节约工时（从 case_metrics_detail 读取）
            io.vanguard.testops.plan.domain.TestPlanCaseMetrics metrics = new io.vanguard.testops.plan.domain.TestPlanCaseMetrics();
            metrics.setId(IDGenerator.nextStr());
            metrics.setTestPlanId(testPlanId);
            metrics.setCaseId(functionalCaseId);
            metrics.setProjectId(projectId);
            metrics.setCaseSourceType(sourceType);
            metrics.setModificationCostMs(modificationCost); // 快照值，从 case_metrics_detail 读取
            metrics.setSavedWriteMs(savedWriteMs); // 快照值，从 case_metrics_detail 读取
            
            // Snapshot CS Score
            if (csDetail != null) {
                metrics.setSnapshotCsScore(csDetail.getCsScore());
                metrics.setSnapshotLevel(csDetail.getComplexityLevel());
            }
            
            metrics.setExecCount(0);
            metrics.setIsBlockedRun(false);
            metrics.setActualExecMs(0L);
            metrics.setActualReadingMs(0L);
            metrics.setTotalTimeMs(0L); // 初始总耗时为0
            metrics.setFocusOutCount(0);
            metrics.setFilteredTimeMs(0L);
            metrics.setIsBatchFill(0);
            metrics.setCreateTime(System.currentTimeMillis());
            metrics.setUpdateTime(System.currentTimeMillis());
            
            testPlanCaseMetricsMapper.insert(metrics);
            
        } catch (Exception e) {
            io.vanguard.testops.sdk.util.LogUtils.error("Failed to create test plan case metrics for case: " + testPlanCase.getFunctionalCaseId(), e);
        }
    }

    private String determineCaseSourceType(String functionalCaseId, String currentTestPlanId) {
        // Check if used in other test plans
        TestPlanFunctionalCaseExample example = new TestPlanFunctionalCaseExample();
        example.createCriteria()
                .andFunctionalCaseIdEqualTo(functionalCaseId)
                .andTestPlanIdNotEqualTo(currentTestPlanId); // Exclude current plan
        
        long count = testPlanFunctionalCaseMapper.countByExample(example);
        
        if (count == 0) {
            return "NEW";
        }
        
        // Check for modifications
        List<io.vanguard.testops.functional.domain.CaseChangeLog> logs = caseChangeLogMapper.selectByCaseId(functionalCaseId);
        
        if (CollectionUtils.isNotEmpty(logs)) {
            return "MODIFY";
        } else {
            return "REUSE";
        }
    }

    /**
     * 构建测试计划功能用例对象
     *
     * @param testPlan
     * @param functional
     * @param user
     * @param testPlanFunctionalCaseList
     */
    private void buildTestPlanFunctionalCase(TestPlan testPlan, BaseCollectionAssociateRequest functional, SessionUser user, List<TestPlanFunctionalCase> testPlanFunctionalCaseList, boolean isRepeat) {
        super.checkCollection(testPlan.getId(), functional.getCollectionId(), CaseType.FUNCTIONAL_CASE.getKey());
        boolean selectAllModule = functional.getModules().isSelectAllModule();
        Map<String, ModuleSelectDTO> moduleMaps = functional.getModules().getModuleMaps();
        moduleMaps.remove(MODULE_ALL);
        if (selectAllModule) {
            // 选择了全部模块
            List<FunctionalCase> functionalCaseList = extFunctionalCaseMapper.selectAllFunctionalCase(isRepeat, functional.getModules().getProjectId(), testPlan.getId());
            buildTestPlanFunctionalCaseDTO(functional, functionalCaseList, testPlan, user, testPlanFunctionalCaseList);
            handleSyncCase(isRepeat, functionalCaseList, functional, testPlan, user);
        } else {
            AssociateCaseDTO dto = super.getCaseIds(moduleMaps);
            List<FunctionalCase> functionalCaseList = new ArrayList<>();
            //获取全选的模块数据
            if (CollectionUtils.isNotEmpty(dto.getModuleIds())) {
                functionalCaseList = extFunctionalCaseMapper.getListBySelectModules(isRepeat, functional.getModules().getProjectId(), dto.getModuleIds(), testPlan.getId());
            }

            if (CollectionUtils.isNotEmpty(dto.getSelectIds())) {
                CollectionUtils.removeAll(dto.getSelectIds(), functionalCaseList.stream().map(FunctionalCase::getId).toList());
                //获取选中的ids数据
                List<FunctionalCase> selectIdList = extFunctionalCaseMapper.getListBySelectIds(functional.getModules().getProjectId(), dto.getSelectIds(), testPlan.getId());
                functionalCaseList.addAll(selectIdList);
            }

            if (CollectionUtils.isNotEmpty(dto.getExcludeIds())) {
                //排除的ids
                List<String> excludeIds = dto.getExcludeIds();
                functionalCaseList = functionalCaseList.stream().filter(item -> !excludeIds.contains(item.getId())).toList();
            }

            if (CollectionUtils.isNotEmpty(functionalCaseList)) {
                List<FunctionalCase> list = functionalCaseList.stream().sorted(Comparator.comparing(FunctionalCase::getPos)).toList();
                buildTestPlanFunctionalCaseDTO(functional, list, testPlan, user, testPlanFunctionalCaseList);
                handleSyncCase(isRepeat, functionalCaseList, functional, testPlan, user);
            }

        }
    }


    /**
     * 处理同步添加功能用例关联的用例
     *
     * @param isRepeat
     * @param functionalCaseList
     * @param functional
     * @param testPlan
     * @param user
     */
    private void handleSyncCase(boolean isRepeat, List<FunctionalCase> functionalCaseList, BaseCollectionAssociateRequest functional, TestPlan testPlan, SessionUser user) {
        if (BooleanUtils.isTrue(functional.getModules().isSyncCase())) {
            handleApiCaseData(isRepeat, functionalCaseList, functional, testPlan, user);
            handleApiScenarioData(isRepeat, functionalCaseList, functional, testPlan, user);
        }
    }

    /**
     * 处理场景用例数据
     *
     * @param isRepeat
     * @param functionalCaseList
     * @param functional
     * @param testPlan
     * @param user
     */
    private void handleApiScenarioData(boolean isRepeat, List<FunctionalCase> functionalCaseList, BaseCollectionAssociateRequest functional, TestPlan testPlan, SessionUser user) {
        if (StringUtils.isNotBlank(functional.getModules().getApiScenarioCollectionId()) && checkApiCollection(testPlan, functional.getModules().getApiScenarioCollectionId(), CaseType.SCENARIO_CASE.getKey())) {
            List<String> caseIds = functionalCaseList.stream().map(FunctionalCase::getId).toList();
            List<ApiScenario> scenarioList = extFunctionalCaseTestMapper.selectApiScenarioByCaseIds(isRepeat, caseIds, testPlan.getId());
            List<TestPlanApiScenario> testPlanApiScenarioList = new ArrayList<>();
            testPlanApiScenarioService.buildTestPlanApiScenarioDTO(functional.getModules().getApiScenarioCollectionId(), scenarioList, testPlan, user, testPlanApiScenarioList);
            if (CollectionUtils.isNotEmpty(testPlanApiScenarioList)) {
                testPlanApiScenarioMapper.batchInsert(testPlanApiScenarioList);
            }
        }
    }

    /**
     * 处理接口用例数据
     *
     * @param functionalCaseList
     * @param functional
     * @param testPlan
     * @param user
     */
    private void handleApiCaseData(boolean isRepeat, List<FunctionalCase> functionalCaseList, BaseCollectionAssociateRequest functional, TestPlan testPlan, SessionUser user) {
        if (StringUtils.isNotBlank(functional.getModules().getApiCaseCollectionId()) && checkApiCollection(testPlan, functional.getModules().getApiCaseCollectionId(), CaseType.API_CASE.getKey())) {
            List<String> caseIds = functionalCaseList.stream().map(FunctionalCase::getId).toList();
            List<ApiTestCase> apiTestCaseList = extFunctionalCaseTestMapper.selectApiCaseByCaseIds(isRepeat, caseIds, testPlan.getId());
            List<TestPlanApiCase> testPlanApiCaseList = new ArrayList<>();
            testPlanApiCaseService.buildTestPlanApiCaseDTO(functional.getModules().getApiCaseCollectionId(), apiTestCaseList, testPlan, user, testPlanApiCaseList);
            if (CollectionUtils.isNotEmpty(testPlanApiCaseList)) {
                testPlanApiCaseMapper.batchInsert(testPlanApiCaseList);
            }
        }
    }

    /**
     * 校验测试集
     *
     * @param testPlan
     * @param apiCaseCollectionId
     * @return
     */
    private boolean checkApiCollection(TestPlan testPlan, String apiCaseCollectionId, String type) {
        TestPlanCollectionExample collectionExample = new TestPlanCollectionExample();
        collectionExample.createCriteria().andIdEqualTo(apiCaseCollectionId).andTestPlanIdEqualTo(testPlan.getId()).andTypeEqualTo(type);
        return testPlanCollectionMapper.countByExample(collectionExample) > 0;
    }

    private void buildTestPlanFunctionalCaseDTO(BaseCollectionAssociateRequest functional, List<FunctionalCase> functionalCaseList, TestPlan testPlan, SessionUser user, List<TestPlanFunctionalCase> testPlanFunctionalCaseList) {
        AtomicLong nextOrder = new AtomicLong(getNextOrder(functional.getCollectionId()));
        functionalCaseList.forEach(functionalCase -> {
            TestPlanFunctionalCase testPlanFunctionalCase = new TestPlanFunctionalCase();
            testPlanFunctionalCase.setId(IDGenerator.nextStr());
            testPlanFunctionalCase.setTestPlanCollectionId(functional.getCollectionId());
            testPlanFunctionalCase.setTestPlanId(testPlan.getId());
            testPlanFunctionalCase.setFunctionalCaseId(functionalCase.getId());
            testPlanFunctionalCase.setCreateUser(user.getId());
            testPlanFunctionalCase.setCreateTime(System.currentTimeMillis());
            testPlanFunctionalCase.setPos(nextOrder.getAndAdd(DEFAULT_NODE_INTERVAL_POS));
            testPlanFunctionalCase.setLastExecResult(ExecStatus.PENDING.name());
            testPlanFunctionalCaseList.add(testPlanFunctionalCase);
        });
    }

    @Override
    public void initResourceDefaultCollection(String planId, List<TestPlanCollectionDTO> defaultCollections) {
        TestPlanCollectionDTO defaultCollection = defaultCollections.stream().filter(collection -> StringUtils.equals(collection.getType(), CaseType.FUNCTIONAL_CASE.getKey())
                && !StringUtils.equals(collection.getParentId(), "NONE")).toList().getFirst();
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        TestPlanFunctionalCaseMapper functionalBatchMapper = sqlSession.getMapper(TestPlanFunctionalCaseMapper.class);
        TestPlanFunctionalCase record = new TestPlanFunctionalCase();
        record.setTestPlanCollectionId(defaultCollection.getId());
        TestPlanFunctionalCaseExample functionalCaseExample = new TestPlanFunctionalCaseExample();
        functionalCaseExample.createCriteria().andTestPlanIdEqualTo(planId);
        functionalBatchMapper.updateByExampleSelective(record, functionalCaseExample);
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }

    /**
     * 批量移动
     *
     * @param request
     */
    public void batchMove(BaseBatchMoveRequest request) {
        List<String> ids = doSelectIds(request);
        if (CollectionUtils.isNotEmpty(ids)) {
            moveCaseToCollection(ids, request.getTargetCollectionId());
        }
    }

    private void moveCaseToCollection(List<String> ids, String targetCollectionId) {
        AtomicLong nextOrder = new AtomicLong(getNextOrder(targetCollectionId));
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        TestPlanFunctionalCaseMapper functionalBatchMapper = sqlSession.getMapper(TestPlanFunctionalCaseMapper.class);
        ids.forEach(id -> {
            TestPlanFunctionalCase testPlanFunctionalCase = new TestPlanFunctionalCase();
            testPlanFunctionalCase.setId(id);
            testPlanFunctionalCase.setPos(nextOrder.getAndAdd(DEFAULT_NODE_INTERVAL_POS));
            testPlanFunctionalCase.setTestPlanCollectionId(targetCollectionId);
            functionalBatchMapper.updateByPrimaryKeySelective(testPlanFunctionalCase);
        });
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }

    public Map<String, List<String>> getFuncCaseAssociationCaseMap(List<String> functionalCaseIds) {
        if (CollectionUtils.isEmpty(functionalCaseIds)) {
            return new HashMap<>();
        }
        List<FunctionalCaseTest> functionalCaseTestList = extFunctionalCaseTestMapper.selectApiAndScenarioIdsFromCaseIds(functionalCaseIds);
        return functionalCaseTestList.stream().collect(Collectors.groupingBy(FunctionalCaseTest::getCaseId, Collectors.mapping(FunctionalCaseTest::getSourceId, Collectors.toList())));
    }

    /**
     * 处理执行人为空过滤参数
     *
     * @param filter 请求参数
     */
    protected boolean filterCaseRequest(Map<String, List<String>> filter) {
        if (filter != null && filter.containsKey(EXECUTOR)) {
            List<String> filterExecutorIds = filter.get(EXECUTOR);
            if (CollectionUtils.isNotEmpty(filterExecutorIds)) {
                return filterExecutorIds.contains("-");
            }
        }
        return false;
    }

    public void batchAssociateBug(TestPlanCaseBatchAddBugRequest request, String bugId, String userId) {
        List<String> ids = doSelectIds(request);
        if (CollectionUtils.isNotEmpty(ids)) {
            handleAssociateBug(ids, userId, bugId, request.getTestPlanId());

        }
    }

    public void handleAssociateBug(List<String> ids, String userId, String bugId, String testPlanId) {
        SubListUtils.dealForSubList(ids, 500, (subList) -> {
            Map<String, String> caseMap = getCaseMap(subList);
            List<BugRelationCase> list = new ArrayList<>();
            subList.forEach(id -> {
                BugRelationCase bugRelationCase = new BugRelationCase();
                bugRelationCase.setId(IDGenerator.nextStr());
                bugRelationCase.setBugId(bugId);
                bugRelationCase.setCaseId(caseMap.get(id));
                bugRelationCase.setCaseType(CaseType.FUNCTIONAL_CASE.getKey());
                bugRelationCase.setCreateUser(userId);
                bugRelationCase.setCreateTime(System.currentTimeMillis());
                bugRelationCase.setUpdateTime(System.currentTimeMillis());
                bugRelationCase.setTestPlanCaseId(id);
                bugRelationCase.setTestPlanId(testPlanId);
                list.add(bugRelationCase);
            });
            bugRelationCaseMapper.batchInsert(list);
        });
    }

    public Map<String, String> getCaseMap(List<String> ids) {
        TestPlanFunctionalCaseExample example = new TestPlanFunctionalCaseExample();
        example.createCriteria().andIdIn(ids);
        List<TestPlanFunctionalCase> caseList = testPlanFunctionalCaseMapper.selectByExample(example);
        return caseList.stream().collect(Collectors.toMap(TestPlanFunctionalCase::getId, TestPlanFunctionalCase::getFunctionalCaseId));
    }


    public void batchAssociateBugByIds(TestPlanCaseBatchAssociateBugRequest request, String userId) {
        List<String> ids = doSelectIds(request);
        if (CollectionUtils.isNotEmpty(ids)) {
            handleAssociateBugByIds(ids, request, userId);
        }
    }

    public void handleAssociateBugByIds(List<String> ids, TestPlanCaseBatchAssociateBugRequest request, String userId) {
        SubListUtils.dealForSubList(ids, 500, (subList) -> {
            BugRelationCaseExample example = new BugRelationCaseExample();
            example.createCriteria().andTestPlanCaseIdIn(subList).andTestPlanIdEqualTo(request.getTestPlanId()).andBugIdIn(request.getBugIds());
            List<BugRelationCase> bugRelationCases = bugRelationCaseMapper.selectByExample(example);
            Map<String, List<String>> bugMap = bugRelationCases.stream()
                    .collect(Collectors.groupingBy(
                            BugRelationCase::getTestPlanCaseId,
                            Collectors.mapping(BugRelationCase::getBugId, Collectors.toList())
                    ));
            Map<String, String> caseMap = getCaseMap(subList);
            List<BugRelationCase> list = new ArrayList<>();
            subList.forEach(item -> {
                buildAssociateBugData(item, bugMap, list, request, caseMap, userId);
            });
            if (CollectionUtils.isNotEmpty(list)) {
                bugRelationCaseMapper.batchInsert(list);
            }
        });

    }

    private void buildAssociateBugData(String id, Map<String, List<String>> bugMap, List<BugRelationCase> list, TestPlanCaseBatchAssociateBugRequest request, Map<String, String> caseMap, String userId) {
        List<String> bugIds = new ArrayList<>(request.getBugIds());
        if (bugMap.containsKey(id)) {
            bugIds.removeAll(bugMap.get(id));
        }
        bugIds.forEach(bugId -> {
            BugRelationCase bugRelationCase = new BugRelationCase();
            bugRelationCase.setId(IDGenerator.nextStr());
            bugRelationCase.setBugId(bugId);
            bugRelationCase.setCaseId(caseMap.get(id));
            bugRelationCase.setCaseType(CaseType.FUNCTIONAL_CASE.getKey());
            bugRelationCase.setCreateUser(userId);
            bugRelationCase.setCreateTime(System.currentTimeMillis());
            bugRelationCase.setUpdateTime(System.currentTimeMillis());
            bugRelationCase.setTestPlanCaseId(id);
            bugRelationCase.setTestPlanId(request.getTestPlanId());
            list.add(bugRelationCase);
        });
    }

    /**
     * 重新分配批量执行用例的时间
     * 当天所有批量提交的用例共享 7.5 小时
     * 
     * @param testPlanId 测试计划ID
     * @param currentTime 当前时间
     */
    private void redistributeBatchExecutionTime(String testPlanId, long currentTime) {
        try {
            // 计算今天的开始和结束时间（凌晨0点到23:59:59）
            long todayStart = getTodayStartTime(currentTime);
            long todayEnd = getTodayEndTime(currentTime);
            
            // 查询当天该测试计划下所有批量提交的用例（is_batch_fill=1）
            List<io.vanguard.testops.plan.domain.TestPlanCaseMetrics> batchCases = 
                testPlanCaseMetricsMapper.selectBatchCasesByPlanAndDate(testPlanId, todayStart, todayEnd);
            
            if (batchCases == null || batchCases.isEmpty()) {
                return;
            }
            
            int batchCount = batchCases.size();
            
            // 7.5小时 = 7.5 * 60 * 60 * 1000 毫秒
            long totalBatchTimeMs = (long) (7.5 * 60 * 60 * 1000);
            
            // 平均分配时间
            long timePerCase = totalBatchTimeMs / batchCount;
            
            io.vanguard.testops.sdk.util.LogUtils.info("重新分配批量用例时间: testPlanId={}, 当天批量用例数={}, 每个用例分配时间={}ms ({}分钟)", 
                    testPlanId, batchCount, timePerCase, timePerCase / 60000);
            
            // 更新所有批量用例的总耗时
            for (io.vanguard.testops.plan.domain.TestPlanCaseMetrics metrics : batchCases) {
                metrics.setTotalTimeMs(timePerCase);
                // actual_exec_ms 和 actual_reading_ms 保持为 0（批量提交不记录真实时间）
                metrics.setUpdateTime(currentTime);
                testPlanCaseMetricsMapper.updateByPrimaryKey(metrics);
            }
            
            io.vanguard.testops.sdk.util.LogUtils.info("批量用例时间分配完成: testPlanId={}, 更新了{}个用例", testPlanId, batchCount);
            
        } catch (Exception e) {
            io.vanguard.testops.sdk.util.LogUtils.error("重新分配批量用例时间失败: testPlanId=" + testPlanId, e);
        }
    }
    
    /**
     * 获取当天开始时间（凌晨0点）
     */
    private long getTodayStartTime(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    /**
     * 获取当天结束时间（23:59:59.999）
     */
    private long getTodayEndTime(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}
