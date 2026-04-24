package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.dto.CaseDetailWithCSDTO;
import io.vanguard.testops.functional.dto.CaseListResponseDTO;
import io.vanguard.testops.functional.dto.CaseMetricsDTO;
import io.vanguard.testops.functional.dto.CSMetricsDTO;
import io.vanguard.testops.functional.request.CaseListQueryRequest;
import io.vanguard.testops.functional.request.CaseMetricsQueryRequest;
import io.vanguard.testops.functional.request.CSMetricsQueryRequest;
import io.vanguard.testops.functional.service.CaseMetricsService;
import io.vanguard.testops.functional.service.CaseCSCalculationService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.validation.groups.Created;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用例效能指标Controller
 */
@RestController
@RequestMapping("/functional/case/metrics")
@Tag(name = "用例效能指标")
public class CaseMetricsController {

    @Resource
    private CaseMetricsService caseMetricsService;
    
    @Resource
    private CaseCSCalculationService caseCSCalculationService;

    /**
     * 获取综合指标（需要鉴权）
     */
    @PostMapping("/comprehensive")
    @Operation(summary = "获取用例效能综合指标")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public CaseMetricsDTO getComprehensiveMetrics(@Validated(Created.class) @RequestBody CaseMetricsQueryRequest request) {
        return caseMetricsService.getComprehensiveMetrics(request);
    }

    /**
     * 获取综合指标（公开接口，无需鉴权，用于aegis-one-web）
     */
    @PostMapping("/comprehensive/public")
    @Operation(summary = "获取用例效能综合指标（公开接口）")
    public CaseMetricsDTO getComprehensiveMetricsPublic(@Validated(Created.class) @RequestBody CaseMetricsQueryRequest request) {
        return caseMetricsService.getComprehensiveMetrics(request);
    }

    /**
     * 获取项目级指标（公开接口）
     */
    @GetMapping("/project/{projectId}/public")
    @Operation(summary = "获取项目级指标（公开接口）")
    public CaseMetricsDTO getProjectMetricsPublic(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "WEEK") String timeDimension,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        
        CaseMetricsQueryRequest request = new CaseMetricsQueryRequest();
        request.setProjectId(projectId);
        request.setMetricLevel("PROJECT");
        request.setTimeDimension(timeDimension);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        
        return caseMetricsService.getComprehensiveMetrics(request);
    }

    /**
     * 获取CS复杂分指标（公开接口，用于aegis-one-web）
     */
    @PostMapping("/cs/comprehensive")
    @Operation(summary = "获取CS复杂分效能指标")
    public CSMetricsDTO getCSMetrics(@Validated @RequestBody CSMetricsQueryRequest request) {
        return caseMetricsService.getCSMetrics(request);
    }

    /**
     * 计算单个用例的CS分值
     */
    @GetMapping("/cs/calculate/{caseId}")
    @Operation(summary = "计算单个用例的CS分值")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    public BigDecimal calculateCaseCS(@PathVariable String caseId) {
        return caseMetricsService.calculateCaseCS(caseId);
    }

    /**
     * 批量计算CS值（用于初始化或重新计算）
     * @param projectId 项目ID，如果为null则计算所有项目
     * @param forceRecalculate 是否强制重新计算（即使已存在缓存）
     * @return 计算成功的用例数量
     */
    @PostMapping("/cs/batch-calculate")
    @Operation(summary = "批量计算用例CS值（受限）")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    public Map<String, Object> batchCalculateCS(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean forceRecalculate) {
        // 获取总数
        int totalCount = caseCSCalculationService.getTotalCaseCount(projectId);
        
        // 触发异步计算
        caseCSCalculationService.batchCalculateMetricsDetailAsync(projectId, forceRecalculate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("message", "批量计算CS值任务已提交，共 " + totalCount + " 个用例");
        result.put("async", true);
        return result;
    }

    /**
     * 批量计算并写入 case_metrics_detail 表（公开接口，无需鉴权，异步执行）
     */
    @PostMapping("/cs/batch-calculate/detail")
    @Operation(summary = "批量计算CS并写入 case_metrics_detail（公开，异步执行）")
    public Map<String, Object> batchCalculateMetricsDetail(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean forceRecalculate) {
        
        // 先获取总数（同步查询，很快）
        int totalCount = caseCSCalculationService.getTotalCaseCount(projectId);
        
        // 触发异步计算（立即返回）
        caseCSCalculationService.batchCalculateMetricsDetailAsync(projectId, forceRecalculate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("totalCount", totalCount);
        result.put("message", "计算任务已提交至后台异步执行，共 " + totalCount + " 个用例");
        result.put("async", true);
        result.put("tip", "任务正在后台执行，不会阻塞其他操作，请稍后查看结果");
        
        return result;
    }
    
    /**
     * 批量计算CS值（公开接口，无需鉴权，用于初始化）
     * 异步执行，立即返回，不阻塞主线程
     * @param projectId 项目ID，如果为null或"all"则计算所有项目
     * @param forceRecalculate 是否强制重新计算（即使已存在缓存）
     * @param batchSize 每批处理的用例数量，默认100（暂未使用，保留兼容性）
     * @return 计算结果统计
     */
    @PostMapping("/cs/batch-calculate/public")
    @Operation(summary = "批量计算用例CS值（公开接口，异步执行）")
    public Map<String, Object> batchCalculateCSPublic(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false, defaultValue = "false") boolean forceRecalculate,
            @RequestParam(required = false, defaultValue = "100") int batchSize) {
        
        // 如果 projectId 为 "all"，则设置为 null 以计算所有项目
        if ("all".equalsIgnoreCase(projectId)) {
            projectId = null;
        }
        
        // 先获取总数（同步查询，很快）
        int totalCount = caseCSCalculationService.getTotalCaseCount(projectId);
        
        // 触发异步计算（立即返回）
        caseCSCalculationService.batchCalculateMetricsDetailAsync(projectId, forceRecalculate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("totalCount", totalCount);
        result.put("message", "计算任务已提交至后台异步执行，共 " + totalCount + " 个用例");
        result.put("async", true);
        result.put("tip", "任务正在后台执行，不会阻塞其他操作，请稍后查看结果");
        result.put("projectId", projectId != null ? projectId : "all");
        result.put("forceRecalculate", forceRecalculate);
        
        return result;
    }

    
    /**
     * 根据指标类型查询用例列表及其CS值（公开接口）
     */
    @PostMapping("/case-list")
    @Operation(summary = "根据指标类型查询用例列表及其CS值")
    public CaseListResponseDTO getCaseListByMetric(@Validated @RequestBody CaseListQueryRequest request) {
        return caseMetricsService.getCaseListByMetric(request);
    }
}

