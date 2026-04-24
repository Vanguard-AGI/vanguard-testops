package io.vanguard.testops.requirementquality.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.requirementquality.domain.RequirementChangeStats;
import io.vanguard.testops.requirementquality.dto.StoryLocDeployAggVO;
import io.vanguard.testops.requirementquality.mapper.ExtRequirementQualityMapper;
import io.vanguard.testops.requirementquality.mapper.RequirementChangeStatsMapper;
import io.vanguard.testops.requirementquality.request.PipelineRecordCreateRequest;
import io.vanguard.testops.requirementquality.request.PipelineRecordListRequest;
import io.vanguard.testops.requirementquality.request.PipelineRecordUpdateRequest;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.mapper.BaseProjectMapper;
import io.vanguard.testops.system.dto.page.Pager;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 门禁管理 - 流水线记录列表与运维补全。
 * 流水线关联需求时（设置/修改 story_id）会触发需求表 meego_story_stats 的 frontend_loc_changed、backend_loc_changed 等汇总字段重算并写回；
 * 纠错改绑时先对原需求重算（从原需求上删除本条贡献），再对新需求重算（累加本条贡献）。
 */
@Service
public class PipelineRecordService {

    @Resource
    private RequirementChangeStatsMapper requirementChangeStatsMapper;

    @Resource
    private ExtRequirementQualityMapper extRequirementQualityMapper;

    @Resource
    private BaseProjectMapper baseProjectMapper;

    /**
     * 分页查询流水线记录（支持按项目、服务、发布结果、时间范围筛选）；列表填充需求名称、项目名称供前端展示
     */
    public Pager<List<RequirementChangeStats>> pageList(PipelineRecordListRequest request) {
        long offset = (long) (request.getCurrent() - 1) * request.getPageSize();
        List<RequirementChangeStats> list = requirementChangeStatsMapper.selectListPage(request, offset);
        long total = requirementChangeStatsMapper.selectCount(request);
        fillStoryAndProjectNames(list);
        return new Pager<>(list, total, request.getPageSize(), request.getCurrent());
    }

    /** 根据 storyId / projectId 批量填充 storyName、projectName（仅展示用） */
    private void fillStoryAndProjectNames(List<RequirementChangeStats> list) {
        if (list == null || list.isEmpty()) return;
        List<String> storyIds = list.stream()
                .map(RequirementChangeStats::getStoryId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        List<String> projectIds = list.stream()
                .map(RequirementChangeStats::getProjectId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> storyNameMap = Collections.emptyMap();
        if (!storyIds.isEmpty()) {
            List<OptionDTO> storyOpts = extRequirementQualityMapper.selectStoryNamesByIds(storyIds);
            storyNameMap = storyOpts != null ? storyOpts.stream().collect(Collectors.toMap(OptionDTO::getId, o -> o.getName() != null ? o.getName() : "", (a, b) -> a)) : Collections.emptyMap();
        }
        Map<String, String> projectNameMap = Collections.emptyMap();
        if (!projectIds.isEmpty()) {
            List<Project> projects = baseProjectMapper.selectProjectByIdList(projectIds);
            projectNameMap = projects != null ? projects.stream().collect(Collectors.toMap(Project::getId, p -> p.getName() != null ? p.getName() : "", (a, b) -> a)) : Collections.emptyMap();
        }
        for (RequirementChangeStats row : list) {
            if (StringUtils.isNotBlank(row.getStoryId())) {
                row.setStoryName(storyNameMap.getOrDefault(row.getStoryId(), row.getStoryId()));
            }
            if (StringUtils.isNotBlank(row.getProjectId())) {
                row.setProjectName(projectNameMap.getOrDefault(row.getProjectId(), row.getProjectId()));
            }
        }
    }

    private static final String ENDPOINT_FRONTEND = "FRONTEND";
    private static final String ENDPOINT_BACKEND = "BACKEND";
    private static final String ENDPOINT_MIXED = "MIXED";

    /**
     * 发布管理 - 手动创建流水线记录并落库，创建后同步需求表聚合指标。
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(PipelineRecordCreateRequest request) {
        RequirementChangeStats row = new RequirementChangeStats();
        row.setId(UUID.randomUUID().toString());
        row.setStoryId(StringUtils.isNotBlank(request.getStoryId()) ? request.getStoryId().trim() : "");
        row.setProjectId(StringUtils.isNotBlank(request.getProjectId()) ? request.getProjectId().trim() : "");
        row.setRepoName(request.getRepoName());
        row.setServiceName(null);
        row.setOtherInfo(null);
        row.setEndpointType(normalizeEndpointType(request.getEndpointType()));
        row.setPipelineId(request.getPipelineId());
        row.setPipelineName(request.getPipelineName());
        row.setPipelineUrl(request.getPipelineUrl() != null ? request.getPipelineUrl().trim() : null);
        row.setEnv(null);
        row.setDeployTime(request.getDeployTime() != null ? request.getDeployTime() : System.currentTimeMillis());
        int locAdd = request.getLocAdd() != null ? Math.max(0, request.getLocAdd()) : 0;
        int locDelete = request.getLocDelete() != null ? Math.max(0, request.getLocDelete()) : 0;
        row.setLocAdd(locAdd);
        row.setLocDelete(locDelete);
        row.setLocModify(0);
        row.setLocValid(locAdd + locDelete);
        row.setDeployResult(request.getDeployResult());
        row.setIsRollback(request.getIsRollback() != null ? request.getIsRollback() : 0);
        row.setIsHotfix(request.getIsHotfix() != null ? request.getIsHotfix() : 0);
        row.setDeployer(request.getDeployer());
        row.setFrontend(request.getFrontend());
        row.setBackend(request.getBackend());
        row.setRemark(request.getRemark());
        row.setDetails(null);
        row.setCreatedAt(System.currentTimeMillis());

        requirementChangeStatsMapper.insert(row);

        if (StringUtils.isNotBlank(row.getStoryId())) {
            syncStoryLocAndDeploy(row.getStoryId());
        }
    }

    private String normalizeEndpointType(String endpointType) {
        if (StringUtils.isBlank(endpointType)) {
            return ENDPOINT_MIXED;
        }
        String t = endpointType.trim().toUpperCase();
        if ("FRONTEND".equals(t)) {
            return ENDPOINT_FRONTEND;
        }
        if ("BACKEND".equals(t)) {
            return ENDPOINT_BACKEND;
        }
        if ("MIXED".equals(t)) {
            return ENDPOINT_MIXED;
        }
        return t;
    }

    /**
     * 门禁管理 - 保存流水线记录（需求ID、项目、环境、发布结果、是否回滚、是否热修等）。
     * 以上字段均支持纠正；每次变更保存后，都会及时对涉及的需求重算并写回需求表聚合指标（增减与流水线表一致），
     * 保证需求表与门禁维护结果一致。
     * 具体：对原关联需求（若有）与新关联需求（若有且不同）各做一次从流水线表的全量重算并写回 meego_story_stats。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(PipelineRecordUpdateRequest request) {
        RequirementChangeStats oldRow = requirementChangeStatsMapper.selectById(request.getId());
        String oldStoryId = oldRow != null && StringUtils.isNotBlank(oldRow.getStoryId()) ? oldRow.getStoryId().trim() : null;
        String newStoryId = StringUtils.isNotBlank(request.getStoryId()) ? request.getStoryId().trim() : null;

        RequirementChangeStats row = new RequirementChangeStats();
        row.setId(request.getId());
        row.setStoryId(newStoryId != null ? newStoryId : "");
        row.setProjectId(request.getProjectId());
        row.setEnv(request.getEnv());
        row.setPipelineUrl(request.getPipelineUrl() != null ? request.getPipelineUrl().trim() : null);
        row.setDeployResult(request.getDeployResult());
        row.setIsRollback(request.getIsRollback());
        row.setIsHotfix(request.getIsHotfix());
        row.setRemark(request.getRemark());
        row.setFrontend(request.getFrontend());
        row.setBackend(request.getBackend());
        requirementChangeStatsMapper.updateById(row);

        if (oldStoryId != null) {
            syncStoryLocAndDeploy(oldStoryId);
        }
        if (newStoryId != null && !newStoryId.equals(oldStoryId)) {
            syncStoryLocAndDeploy(newStoryId);
        }
    }

    /**
     * 按 story_id 从 requirement_change_stats 重算该需求的 frontend_loc_changed、backend_loc_changed 及发布类字段，写回 meego_story_stats。
     * 仅更新已存在的 story_id 行（不存在则 0 行更新）。
     */
    private void syncStoryLocAndDeploy(String storyId) {
        if (StringUtils.isBlank(storyId)) {
            return;
        }
        StoryLocDeployAggVO agg = extRequirementQualityMapper.selectStoryLocDeployAgg(storyId);
        if (agg == null) {
            return;
        }
        int frontend = agg.getFrontendLocChanged() != null ? agg.getFrontendLocChanged() : 0;
        int backend = agg.getBackendLocChanged() != null ? agg.getBackendLocChanged() : 0;
        int deployTotal = agg.getDeployTotalCount() != null ? agg.getDeployTotalCount() : 0;
        int deployFailure = agg.getDeployFailureCount() != null ? agg.getDeployFailureCount() : 0;
        Long lastDeploy = agg.getLastDeployTime();
        BigDecimal changeFailureRate = deployTotal > 0
                ? BigDecimal.valueOf(deployFailure).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(deployTotal), 2, RoundingMode.HALF_UP)
                : null;
        extRequirementQualityMapper.updateMeegoStoryStatsLocDeploy(storyId, frontend, backend, deployTotal, deployFailure, lastDeploy, changeFailureRate);
    }
}
