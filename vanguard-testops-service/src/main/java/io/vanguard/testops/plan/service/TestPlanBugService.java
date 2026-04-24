package io.vanguard.testops.plan.service;

import io.vanguard.testops.bug.domain.BugRelationCaseExample;
import io.vanguard.testops.bug.dto.response.BugCustomFieldDTO;
import io.vanguard.testops.bug.mapper.BugRelationCaseMapper;
import io.vanguard.testops.bug.mapper.ExtBugCustomFieldMapper;
import io.vanguard.testops.bug.service.BugCommonService;
import io.vanguard.testops.plan.dto.TestPlanBugCaseDTO;
import io.vanguard.testops.plan.dto.TestPlanCollectionDTO;
import io.vanguard.testops.plan.dto.TestPlanResourceExecResultDTO;
import io.vanguard.testops.plan.dto.request.BaseCollectionAssociateRequest;
import io.vanguard.testops.plan.dto.request.TestPlanBugPageRequest;
import io.vanguard.testops.plan.dto.response.TestPlanBugPageResponse;
import io.vanguard.testops.plan.mapper.ExtTestPlanBugMapper;
import io.vanguard.testops.plugin.platform.dto.SelectOption;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanBugService extends TestPlanResourceService {

    @Resource
    private BaseUserMapper baseUserMapper;
    @Resource
    private ExtTestPlanBugMapper extTestPlanBugMapper;
    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private BugCommonService bugCommonService;
    @Resource
    private ExtBugCustomFieldMapper extBugCustomFieldMapper;

    public List<TestPlanBugPageResponse> page(TestPlanBugPageRequest request) {
        List<TestPlanBugPageResponse> bugList = extTestPlanBugMapper.list(request);
        if (CollectionUtils.isEmpty(bugList)) {
            return new ArrayList<>();
        }
        parseCustomField(bugList, request.getProjectId());
		return buildBugRelatedListExtraInfo(bugList, request.getPlanId());
    }

    @Override
    public void deleteBatchByTestPlanId(List<String> testPlanIdList) {
        BugRelationCaseExample example = new BugRelationCaseExample();
        example.createCriteria().andTestPlanIdIn(testPlanIdList);
        bugRelationCaseMapper.deleteByExample(example);
    }

    @Override
    public Map<String, Long> caseExecResultCount(String testPlanId) {
        return Map.of();
    }

    @Override
    public long copyResource(String originalTestPlanId, String newTestPlanId, Map<String, String> oldCollectionIdToNewCollectionId, String operator, long operatorTime) {
        return 0;
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectDistinctExecResultByProjectId(String projectId) {
        return List.of();
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectLastExecResultByProjectId(String projectId) {
        return List.of();
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectDistinctExecResultByTestPlanIds(List<String> testPlanIds) {
        return List.of();
    }

    @Override
    public List<TestPlanResourceExecResultDTO> selectLastExecResultByTestPlanIds(List<String> testPlanIds) {
        return List.of();
    }


    @Override
    public long getNextOrder(String testPlanId) {
        return 0;
    }

    @Override
    public void updatePos(String id, long pos) {

    }

    @Override
    public void refreshPos(String testPlanId) {

    }

    /**
     * 处理自定义字段
     *
     * @param bugList 缺陷集合
     */
    private void parseCustomField(List<TestPlanBugPageResponse> bugList, String projectId) {
        // MS处理人会与第三方的值冲突, 分开查询
        List<SelectOption> headerOptions = bugCommonService.getHeaderHandlerOption(projectId);
        Map<String, String> headerHandleUserMap = CollectionUtils.isEmpty(headerOptions) ? new HashMap<>() : headerOptions.stream().collect(Collectors.toMap(SelectOption::getValue, SelectOption::getText));
        List<SelectOption> localOptions = bugCommonService.getLocalHandlerOption(projectId);
        Map<String, String> localHandleUserMap = localOptions.stream().collect(Collectors.toMap(SelectOption::getValue, SelectOption::getText));

        Map<String, String> allStatusMap = bugCommonService.getAllStatusMap(projectId);
        bugList.forEach(bug -> {
            // 解析处理人：有展示名则用展示名，否则保留原值（避免被置空导致前端显示 -）
            String handleUserName = headerHandleUserMap.get(bug.getHandleUser());
            if (handleUserName == null && bug.getHandleUser() != null) {
                handleUserName = localHandleUserMap.get(bug.getHandleUser());
            }
            if (handleUserName != null) {
                bug.setHandleUser(handleUserName);
            }
            // 解析状态：有展示名则用展示名，否则保留原状态码
            String statusName = allStatusMap.get(bug.getStatus());
            if (statusName != null) {
                bug.setStatus(statusName);
            }
        });
        // 优先级：从自定义字段 severity 填充
        List<String> bugIds = bugList.stream().map(TestPlanBugPageResponse::getId).toList();
        if (CollectionUtils.isNotEmpty(bugIds)) {
            List<BugCustomFieldDTO> customFields = extBugCustomFieldMapper.getBugAllCustomFields(bugIds, projectId);
            Map<String, String> bugSeverityMap = customFields.stream()
                    .filter(f -> StringUtils.equalsAnyIgnoreCase(f.getId(), "severity", "priority"))
                    .collect(Collectors.toMap(BugCustomFieldDTO::getBugId, BugCustomFieldDTO::getValue, (a, b) -> a));
            bugList.forEach(bug -> bug.setSeverity(bugSeverityMap.get(bug.getId())));
        }
    }

    /**
     * 补充计划-缺陷列表额外信息
     * @param bugList 缺陷列表
     * @return 缺陷列表全部信息
     */
    private List<TestPlanBugPageResponse> buildBugRelatedListExtraInfo(List<TestPlanBugPageResponse> bugList, String planId) {
        // 获取用户集合
        List<String> userIds = new ArrayList<>(bugList.stream().map(TestPlanBugPageResponse::getCreateUser).distinct().toList());
        List<OptionDTO> userOptions = baseUserMapper.selectUserOptionByIds(userIds);
        Map<String, String> userMap = userOptions.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
        List<String> bugIds = bugList.stream().map(TestPlanBugPageResponse::getId).toList();
        List<TestPlanBugCaseDTO> bugRelatedCases = extTestPlanBugMapper.getBugRelatedCase(bugIds, planId);
        Map<String, List<TestPlanBugCaseDTO>> bugRelateCaseMap = bugRelatedCases.stream().collect(Collectors.groupingBy(TestPlanBugCaseDTO::getBugId));
        bugList.forEach(bug -> {
            bug.setRelateCases(bugRelateCaseMap.get(bug.getId()));
            bug.setCreateUser(userMap.get(bug.getCreateUser()));
        });
        return bugList;
    }

    @Override
    public void associateCollection(String planId, Map<String, List<BaseCollectionAssociateRequest>> collectionAssociates, SessionUser user) {
        // TODO: 暂不支持缺陷关联测试集
    }

	@Override
	public void initResourceDefaultCollection(String planId, List<TestPlanCollectionDTO> defaultCollections) {
		// 暂不支持缺陷关联测试集
	}
}
