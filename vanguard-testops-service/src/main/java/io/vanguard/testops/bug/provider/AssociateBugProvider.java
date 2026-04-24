package io.vanguard.testops.bug.provider;


import io.vanguard.testops.bug.domain.BugRelationCase;
import io.vanguard.testops.bug.mapper.BugRelationCaseMapper;
import io.vanguard.testops.bug.mapper.ExtBugMapper;
import io.vanguard.testops.bug.mapper.ExtBugRelateCaseMapper;
import io.vanguard.testops.bug.service.BugCommonService;
import io.vanguard.testops.bug.service.BugRelateCaseCommonService;
import io.vanguard.testops.bug.service.BugStatusService;
import io.vanguard.testops.dto.BugProviderDTO;
import io.vanguard.testops.plugin.platform.dto.SelectOption;
import io.vanguard.testops.provider.BaseAssociateBugProvider;
import io.vanguard.testops.request.AssociateBugPageRequest;
import io.vanguard.testops.request.AssociateBugRequest;
import io.vanguard.testops.request.BugPageProviderRequest;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.service.UserLoginService;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class AssociateBugProvider implements BaseAssociateBugProvider {

    @Resource
    private BugCommonService bugCommonService;
    @Resource
    private BugStatusService bugStatusService;
    @Resource
    private ExtBugMapper extBugMapper;
    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private BugRelateCaseCommonService bugRelateCaseCommonService;
    @Resource
    private ExtBugRelateCaseMapper extBugRelateCaseMapper;
    @Resource
    private UserLoginService userLoginService;


    @Override
    public List<BugProviderDTO> getBugList(String sourceType, String sourceName, String bugColumnName, BugPageProviderRequest bugPageProviderRequest) {
        List<BugProviderDTO> associateBugs = extBugMapper.listByProviderRequest(sourceType, sourceName, bugColumnName, bugPageProviderRequest, false);
        return buildAssociateBugs(associateBugs, bugPageProviderRequest.getProjectId());
    }

    @Override
    public List<String> getSelectBugs(AssociateBugRequest request, boolean deleted) {
        if (request.isSelectAll()) {
            List<String> ids = extBugMapper.getIdsByProvider(request, deleted);
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                ids.removeAll(request.getExcludeIds());
            }
            return ids;
        } else {
            return request.getSelectIds();
        }
    }

    @Override
    public void handleAssociateBug(List<String> ids, String userId, String caseId) {
        List<BugRelationCase> list = new ArrayList<>();
        ids.forEach(id -> {
            BugRelationCase bugRelationCase = new BugRelationCase();
            bugRelationCase.setId(IDGenerator.nextStr());
            bugRelationCase.setBugId(id);
            bugRelationCase.setCaseId(caseId);
            bugRelationCase.setCaseType("FUNCTIONAL");
            bugRelationCase.setCreateUser(userId);
            bugRelationCase.setCreateTime(System.currentTimeMillis());
            bugRelationCase.setUpdateTime(System.currentTimeMillis());
            list.add(bugRelationCase);
        });
        bugRelationCaseMapper.batchInsert(list);
    }

    @Override
    public void disassociateBug(String id) {
        bugRelateCaseCommonService.unRelate(id);
    }

    @Override
    public List<BugProviderDTO> hasAssociateBugPage(AssociateBugPageRequest request) {
        List<BugProviderDTO> associateBugs = extBugRelateCaseMapper.getAssociateBugs(request, request.getSortString());
        associateBugs.forEach(item -> {
            if (StringUtils.isNotBlank(item.getTestPlanName())) {
                item.setSource(Translator.get("test_plan_relate"));
            } else {
                item.setSource(Translator.get("direct_related"));
            }
        });
        return buildAssociateBugs(associateBugs, request.getProjectId());
    }

    @Override
    public List<BugProviderDTO> hasTestPlanAssociateBugPage(AssociateBugPageRequest request) {
        List<BugProviderDTO> associateBugs = extBugRelateCaseMapper.getTestPlanAssociateBugs(request, request.getSortString());
        return buildAssociateBugs(associateBugs, request.getProjectId());
    }

    /**
     * 关联缺陷列表数据处理
     *
     * @param associateBugs 关联缺陷
     * @param projectId     项目ID
     * @return 关联缺陷列表
     */
    public List<BugProviderDTO> buildAssociateBugs(List<BugProviderDTO> associateBugs, String projectId) {
        List<SelectOption> headerHandlerOption = bugCommonService.getHeaderHandlerOption(projectId);
        List<SelectOption> statusOption = bugStatusService.getHeaderStatusOption(projectId);
        associateBugs.forEach(item -> {
            headerHandlerOption.stream().filter(option -> StringUtils.equals(option.getValue(), item.getHandleUser())).findFirst().ifPresent(option -> item.setHandleUserName(option.getText()));
            statusOption.stream().filter(option -> StringUtils.equals(option.getValue(), item.getStatus())).findFirst().ifPresent(option -> item.setStatusName(option.getText()));
        });
        return associateBugs;
    }
}
