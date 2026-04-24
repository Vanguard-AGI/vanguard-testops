package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.dto.ProjectOverviewDTO;
import io.vanguard.testops.functional.dto.dashboard.RequirementDTO;
import io.vanguard.testops.functional.dto.dashboard.CaseWithRequirementDTO;
import io.vanguard.testops.functional.service.MetricsDashboardService;
import io.vanguard.testops.functional.service.MetricDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 效能数据大屏Controller
 * 提供项目概览和个人统计指标API
 */
@RestController
@RequestMapping("/metrics/dashboard")
@Tag(name = "效能数据大屏")
public class MetricsDashboardController {

    @Resource
    private MetricsDashboardService metricsDashboardService;
    
    @Resource
    private MetricDashboardService metricDashboardService;

    /**
     * 获取项目概览指标（21个核心指标）
     * 支持项目维度和个人维度查询
     *
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param dimension 维度类型：personal(个人) 或 project(项目)，可选
     * @param projectId 项目ID，可选，null或"ALL"表示所有项目
     * @param userId 用户ID，可选，null或"all"表示所有用户
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 项目概览指标列表
     */
    @GetMapping("/project-overview")
    @Operation(summary = "获取项目概览指标")
    public List<ProjectOverviewDTO> getProjectOverview(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricsDashboardService.getProjectOverview(dimension, projectId, userId, startTime, endTime);
    }

    /**
     * 获取个人统计指标
     * 
     * @param projectId 项目ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 个人统计指标列表
     */
    @GetMapping("/personal-stats")
    @Operation(summary = "获取个人统计指标")
    public List<io.vanguard.testops.functional.dto.dashboard.PersonalStatsDTO> getPersonalStats(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricDashboardService.getPersonalStats(projectId, startTime, endTime);
    }

    /**
     * 获取用例变更原因分布统计
     * 
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param projectId 项目ID，可选
     * @param userId 用户ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 变更原因分布统计（CASE_COPY 不参与此统计）
     */
    @GetMapping("/change-reason-distribution")
    @Operation(summary = "获取用例变更原因分布统计")
    public java.util.Map<String, Long> getChangeReasonDistribution(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricsDashboardService.getChangeReasonDistribution(projectId, userId, startTime, endTime);
    }

    /**
     * 获取测试用例执行阻塞原因分布统计
     * 
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param projectId 项目ID，可选
     * @param userId 用户ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 阻塞原因分布统计（6种阻塞原因及其数量）
     */
    @GetMapping("/blocked-reason-distribution")
    @Operation(summary = "获取测试用例执行阻塞原因分布统计")
    public java.util.Map<String, Long> getBlockedReasonDistribution(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricsDashboardService.getBlockedReasonDistribution(projectId, userId, startTime, endTime);
    }

    /**
     * 获取需求列表（支持模糊搜索）
     * 用于效能大屏的需求维度筛选
     * 
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param keyword 搜索关键词（需求ID或需求名称），可选
     * @param projectId 项目ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 需求列表（包含关联的测试计划数和用例数统计）
     */
    @GetMapping("/requirements")
    @Operation(summary = "获取需求列表（支持模糊搜索）")
    public List<RequirementDTO> getRequirementsList(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        
        return metricsDashboardService.getRequirementsList(keyword, projectId, startTime, endTime);
    }

    /**
     * 根据变更原因查询用例列表（含需求信息）
     * 用于变更原因分布饼图的钻取功能
     * 
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param changeReason 变更原因（枚举值之一，含 CASE_COPY）
     * @param projectId 项目ID，可选
     * @param userId 用户ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 用例列表（含关联的需求和测试计划信息）
     */
    @GetMapping("/cases-by-change-reason")
    @Operation(summary = "根据变更原因查询用例列表")
    public List<CaseWithRequirementDTO> getCasesByChangeReason(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam String changeReason,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricsDashboardService.getCasesByChangeReason(
                changeReason, projectId, userId, startTime, endTime);
    }

    /**
     * 根据阻塞原因查询用例列表（含需求信息）
     * 用于阻塞原因分布饼图的钻取功能
     * 
     * @param module 模块标识：spotter_aegis（用例管理模块）
     * @param blockReason 阻塞原因（6种之一）
     * @param projectId 项目ID，可选
     * @param userId 用户ID，可选
     * @param startTime 开始时间戳（毫秒），可选
     * @param endTime 结束时间戳（毫秒），可选
     * @return 用例列表（含关联的需求和测试计划信息）
     */
    @GetMapping("/cases-by-block-reason")
    @Operation(summary = "根据阻塞原因查询用例列表")
    public List<CaseWithRequirementDTO> getCasesByBlockReason(
            @RequestParam(required = false, defaultValue = "spotter_aegis") String module,
            @RequestParam String blockReason,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return metricsDashboardService.getCasesByBlockReason(
                blockReason, projectId, userId, startTime, endTime);
    }
}
