package io.vanguard.testops.plan.controller;

import io.vanguard.testops.plan.service.TestPlanMetricsService;
import io.vanguard.testops.sdk.util.LogUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试计划指标计算控制器（公开接口，无需鉴权）
 */
@RestController
@RequestMapping("/test-plan/metrics")
@Tag(name = "测试计划指标计算")
public class TestPlanMetricsController {

    @Resource
    private TestPlanMetricsService testPlanMetricsService;

    /**
     * 手动触发测试计划指标计算（公开接口，无需鉴权）
     * 
     * @param testPlanId 测试计划ID，如果为空则计算所有测试计划
     * @param projectId 项目ID，如果指定则计算该项目下的所有测试计划
     * @return 计算结果
     */
    @PostMapping("/test-plan/calculate/public")
    @Operation(summary = "手动触发测试计划指标计算（公开接口）")
    public ResponseEntity<Map<String, Object>> calculateTestPlanMetrics(
            @RequestParam(required = false) String testPlanId,
            @RequestParam(required = false) String projectId) {
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (StringUtils.isNotBlank(testPlanId)) {
                // 单个测试计划：同步执行，立即返回结果
                long startTime = System.currentTimeMillis();
                try {
                    testPlanMetricsService.calculateAndSaveMetrics(testPlanId);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    result.put("code", 0);
                    result.put("successCount", 1);
                    result.put("failCount", 0);
                    result.put("duration", duration);
                    result.put("durationSeconds", duration / 1000.0);
                    result.put("message", "计算完成");
                    result.put("async", false);
                    
                    LogUtils.info("手动计算测试计划指标成功: testPlanId={}, 耗时: {}ms", testPlanId, duration);
                } catch (Exception e) {
                    result.put("code", -1);
                    result.put("message", "计算失败: " + e.getMessage());
                    result.put("async", false);
                    LogUtils.error("手动计算测试计划指标失败: testPlanId={}", testPlanId, e);
                }
                
            } else if (StringUtils.isNotBlank(projectId)) {
                // 项目级批量计算：异步执行，立即返回
                int totalCount = testPlanMetricsService.calculateByProjectId(projectId);
                
                result.put("code", 0);
                result.put("totalCount", totalCount);
                result.put("message", "计算任务已提交至后台异步执行，共 " + totalCount + " 个测试计划");
                result.put("async", true);
                result.put("tip", "任务正在后台执行，不会阻塞其他操作，请稍后查看结果");
                
                LogUtils.info("项目 {} 的指标计算任务已提交，共 {} 个测试计划", projectId, totalCount);
                
            } else {
                // 全量批量计算：异步执行，立即返回
                int totalCount = testPlanMetricsService.calculateAll();
                
                result.put("code", 0);
                result.put("totalCount", totalCount);
                result.put("message", "计算任务已提交至后台异步执行，共 " + totalCount + " 个测试计划");
                result.put("async", true);
                result.put("tip", "任务正在后台执行，不会阻塞其他操作，请稍后查看结果");
                
                LogUtils.info("全量指标计算任务已提交，共 {} 个测试计划", totalCount);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            LogUtils.error("手动计算测试计划指标异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "提交失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}

