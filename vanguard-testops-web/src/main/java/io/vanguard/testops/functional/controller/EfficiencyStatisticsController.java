package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.dto.dashboard.EfficiencyOverviewRequest;
import io.vanguard.testops.functional.dto.dashboard.EfficiencyOverviewResponse;
import io.vanguard.testops.functional.dto.dashboard.UserActivityResponse;
import io.vanguard.testops.functional.service.MetadataStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 效率仪数据大盘
 * 基于 metadata_operation_log 的 SnapTest 概览与用户活跃度统计
 */
@RestController
@RequestMapping("/metrics/efficiency")
@Tag(name = "效率仪数据大盘")
public class EfficiencyStatisticsController {

    @Resource
    private MetadataStatisticsService metadataStatisticsService;

    /**
     * 获取效率仪概览数据
     *
     * @param request 请求参数：startDate、endDate、personal；项目搜索按项目传 projectIds: ["id1","id2"]
     * @return 效率仪概览响应
     */
    @PostMapping("/overview")
    @Operation(summary = "获取效率仪概览数据", description = "项目搜索按项目传 projectIds: [\"id1\",\"id2\"]")
    public EfficiencyOverviewResponse getOverview(@RequestBody EfficiencyOverviewRequest request) {
        return metadataStatisticsService.getEfficiencyOverview(request);
    }

    /**
     * 获取用户活跃度（近5天）
     *
     * @param request 请求参数：startDate、endDate、personal；项目搜索按项目传 projectIds: ["id1","id2"]
     * @return 用户活跃度响应
     */
    @PostMapping("/activity")
    @Operation(summary = "获取用户活跃度（近5天）", description = "项目搜索按项目传 projectIds: [\"id1\",\"id2\"]")
    public UserActivityResponse getActivity(@RequestBody EfficiencyOverviewRequest request) {
        return metadataStatisticsService.getUserActivityLast5Days(request);
    }
}
