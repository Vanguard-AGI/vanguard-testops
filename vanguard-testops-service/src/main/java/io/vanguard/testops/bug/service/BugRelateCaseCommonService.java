package io.vanguard.testops.bug.service;

import io.vanguard.testops.bug.domain.BugRelationCase;
import io.vanguard.testops.bug.domain.BugRelationCaseExample;
import io.vanguard.testops.bug.dto.BugCaseCheckResult;
import io.vanguard.testops.bug.dto.request.BugRelatedCasePageRequest;
import io.vanguard.testops.bug.dto.response.BugRelateCaseDTO;
import io.vanguard.testops.bug.mapper.BugRelationCaseMapper;
import io.vanguard.testops.bug.mapper.ExtBugRelateCaseMapper;
import io.vanguard.testops.context.AssociateCaseFactory;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.domain.ProjectExample;
import io.vanguard.testops.project.domain.ProjectVersion;
import io.vanguard.testops.project.domain.ProjectVersionExample;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.project.mapper.ProjectVersionMapper;
import io.vanguard.testops.project.service.ModuleTreeService;
import io.vanguard.testops.provider.BaseAssociateCaseProvider;
import io.vanguard.testops.request.AssociateCaseModuleRequest;
import io.vanguard.testops.request.AssociateOtherCaseRequest;
import io.vanguard.testops.sdk.constants.CaseType;
import io.vanguard.testops.sdk.constants.UserRoleType;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.vanguard.testops.system.service.PermissionCheckService;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class BugRelateCaseCommonService extends ModuleTreeService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ProjectVersionMapper projectVersionMapper;
    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private ExtBugRelateCaseMapper extBugRelateCaseMapper;
    @Resource
    private PermissionCheckService permissionCheckService;

    /**
     * 获取关联用例模块树数量
     * @param request 请求参数
     * @return 模块树集合
     */
    public Map<String, Long> countTree(AssociateCaseModuleRequest request) {
        // 用例类型参数非法校验
        this.checkCaseTypeParamIllegal(request.getSourceType());
        // 统计模块数量不用传模块ID
        request.setModuleIds(null);
        List<ModuleCountDTO> moduleCounts = extBugRelateCaseMapper.countRelateCaseModuleTree(request, false, Objects.requireNonNull(CaseType.getType(request.getSourceType())).getCaseTable());
        List<BaseTreeNode> relateCaseModules = extBugRelateCaseMapper.getRelateCaseModule(request,
                Objects.requireNonNull(CaseType.getType(request.getSourceType())).getCaseTable(), Objects.requireNonNull(CaseType.getType(request.getSourceType())).getModuleTable());
        List<BaseTreeNode> relateCaseModuleWithCount = buildTreeAndCountResource(relateCaseModules, moduleCounts, true,
                Translator.get(Objects.requireNonNull(CaseType.getType(request.getSourceType())).getUnPlanName()));
        Map<String, Long> moduleCountMap = getIdCountMapByBreadth(relateCaseModuleWithCount);
        long total = getAllCount(moduleCounts);
        moduleCountMap.put("total", total);
        return moduleCountMap;
    }

    /**
     * 获取关联用例模块树(不包括数量)
     * @param request 请求参数
     * @return 模块树集合
     */
    public List<BaseTreeNode> getRelateCaseTree(AssociateCaseModuleRequest request) {
        // 用例类型参数非法校验
        this.checkCaseTypeParamIllegal(request.getSourceType());
        List<BaseTreeNode> relateCaseModules = extBugRelateCaseMapper.getRelateCaseModule(request,
                Objects.requireNonNull(CaseType.getType(request.getSourceType())).getCaseTable(), Objects.requireNonNull(CaseType.getType(request.getSourceType())).getModuleTable());
        // 构建模块树层级数量为通用逻辑
        return super.buildTreeAndCountResource(relateCaseModules, true, Translator.get(Objects.requireNonNull(CaseType.getType(request.getSourceType())).getUnPlanName()));
    }

    /**
     * 关联用例
     * @param request 关联用例参数
     * @param deleted 是否删除状态
     * @param currentUser 当前用户
     */
    public void relateCase(AssociateOtherCaseRequest request, boolean deleted, String currentUser) {
        // 用例类型参数非法校验
        this.checkCaseTypeParamIllegal(request.getSourceType());
        // 目前只需根据关联条件获取功能用例ID, 后续扩展
        BaseAssociateCaseProvider caseProvider = AssociateCaseFactory.getInstance(request.getSourceType());
        List<String> relatedIds = caseProvider.getRelatedIdsByParam(request, deleted);
        // 缺陷关联用例通用逻辑
        if (CollectionUtils.isEmpty(relatedIds)) {
            return;
        }
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        BugRelationCaseMapper relationCaseMapper = sqlSession.getMapper(BugRelationCaseMapper.class);
        // 根据用例ID筛选出已直接关联缺陷的用例(防止重复关联)
        BugRelationCaseExample bugRelationCaseExample = new BugRelationCaseExample();
        bugRelationCaseExample.createCriteria().andBugIdEqualTo(request.getSourceId()).andCaseIdIn(relatedIds);
        List<BugRelationCase> bugRelationCases = bugRelationCaseMapper.selectByExample(bugRelationCaseExample);
        Map<String, String> bugRelatedMap = bugRelationCases.stream().collect(Collectors.toMap(BugRelationCase::getCaseId, BugRelationCase::getId));
        for (String relatedId : relatedIds) {
            BugRelationCase record = new BugRelationCase();
            if (bugRelatedMap.containsKey(relatedId)) {
                // 重复关联
                continue;
            }
            // 暂未关联, 新生成关联数据
            record.setId(IDGenerator.nextStr());
            record.setCaseId(relatedId);
            record.setBugId(request.getSourceId());
            record.setCaseType(request.getSourceType());
            record.setCreateUser(currentUser);
            record.setCreateTime(System.currentTimeMillis());
            record.setUpdateTime(System.currentTimeMillis());
            relationCaseMapper.insert(record);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }

    /**
     * 分页查询关联用例列表
     * @param request 请求参数
     */
    public List<BugRelateCaseDTO> page(BugRelatedCasePageRequest request) {
        List<BugRelateCaseDTO> relateCases = extBugRelateCaseMapper.list(request);
        if (CollectionUtils.isEmpty(relateCases)) {
            return new ArrayList<>();
        }
        Map<String, String> projectMap = getProjectMap(relateCases.stream().map(BugRelateCaseDTO::getProjectId).toList());
        Map<String, String> versionMap = getVersionMap(relateCases.stream().map(BugRelateCaseDTO::getVersionId).toList());
        relateCases.forEach(relateCase -> {
            relateCase.setProjectName(projectMap.get(relateCase.getProjectId()));
            relateCase.setVersionName(versionMap.get(relateCase.getVersionId()));
            relateCase.setRelateCaseTypeName(Translator.get(Objects.requireNonNull(CaseType.getType(relateCase.getRelateCaseType())).getType()));
        });
        return relateCases;
    }

    /**
     * 取消关联用例
     * @param id 关联ID
     */
    public void unRelate(String id) {
        checkRelate(id);
        bugRelationCaseMapper.deleteByPrimaryKey(id);
    }

    /**
     * 校验当前用户是否有关联用例的权限
     * @param projectId 项目ID
     * @param currentUser 当前用户
     * @param caseType 用例类型
     */
    public BugCaseCheckResult checkPermission(String projectId, String currentUser, String caseType) {
        // 校验用例类型是否合法
        this.checkCaseTypeParamIllegal(caseType);
        boolean hasPermission = permissionCheckService.userHasSourcePermission(currentUser, projectId, Objects.requireNonNull(CaseType.getType(caseType)).getUsePermission(), UserRoleType.PROJECT.name());
        if (!hasPermission) {
            // 没有该用例的访问权限
            return BugCaseCheckResult.builder().pass(false).msg(Translator.get("bug_relate_case_permission_error")).build();
        }
        return BugCaseCheckResult.builder().pass(true).build();
    }

    /**
     * 校验关联用例
     * @param relateId 关联ID
     */
    private void checkRelate(String relateId) {
        BugRelationCase bugRelationCase = bugRelationCaseMapper.selectByPrimaryKey(relateId);
        if (bugRelationCase == null) {
            throw new MSException(Translator.get("bug_relate_case_not_found"));
        }
    }

    /**
     * 获取项目Map
     * @param projectIds 项目ID集合
     * @return 获取项目Map
     */
    private Map<String, String> getProjectMap(List<String> projectIds) {
        ProjectExample projectExample = new ProjectExample();
        projectExample.createCriteria().andIdIn(projectIds);
        List<Project> projects = projectMapper.selectByExample(projectExample);
        return projects.stream().collect(Collectors.toMap(Project::getId, Project::getName));
    }

    /**
     * 获取版本Map
     * @param versionIds 版本ID集合
     * @return 获取版本Map
     */
    private Map<String, String> getVersionMap(List<String> versionIds) {
        ProjectVersionExample projectVersionExample = new ProjectVersionExample();
        projectVersionExample.createCriteria().andIdIn(versionIds);
        List<ProjectVersion> projectVersions = projectVersionMapper.selectByExample(projectVersionExample);
        return projectVersions.stream().collect(Collectors.toMap(ProjectVersion::getId, ProjectVersion::getName));
    }

    @Override
    public void updatePos(String id, long pos) {

    }

    @Override
    public void refreshPos(String parentId) {

    }

    /**
     * 校验用例类型字段是否合法
     * @param caseType 用例类型
     */
    public void checkCaseTypeParamIllegal(String caseType) {
        if (CaseType.getType(caseType) == null) {
            throw new MSException(Translator.get("unknown_case_type_of_relate_case"));
        }
    }
}
