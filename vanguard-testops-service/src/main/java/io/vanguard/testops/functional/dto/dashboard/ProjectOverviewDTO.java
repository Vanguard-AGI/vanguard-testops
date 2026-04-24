package io.vanguard.testops.functional.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard 项目概览DTO（完整版：20+个指标）
 */
@Data
public class ProjectOverviewDTO {
    @Schema(description = "项目ID")
    private String projectId;
    
    // ========== UQS质量指标 (2个) ==========
    @Schema(description = "用例质量综合评分（UQS）(0-100)")
    private BigDecimal avgUQS;
    
    @Schema(description = "测试计划首次通过率 (%)")
    private BigDecimal firstPassRate;
    
    // ========== 复杂度指标 (4个) ==========
    @Schema(description = "用例编写总复杂分")
    private BigDecimal totalWriteComplexity;
    
    @Schema(description = "用例执行总复杂分")
    private BigDecimal totalExecComplexity;
    
    @Schema(description = "用例平均复杂度")
    private BigDecimal avgComplexity;
    
    @Schema(description = "用例复杂度方差")
    private BigDecimal complexityVariance;
    
    // ========== 工时指标 - 整体偏差 (2个) ==========
    @Schema(description = "平均编写工时偏差率 (%)")
    private BigDecimal avgWriteTimeDeviation;
    
    @Schema(description = "平均执行工时偏差率 (%)")
    private BigDecimal avgExecTimeDeviation;
    
    // ========== 工时指标 - 按复杂度分级 (8个) ==========
    @Schema(description = "预期用例编写时长（按复杂度等级）")
    private TimeByLevel expectedWriteTime;
    
    @Schema(description = "实际用例编写时长（按复杂度等级）")
    private TimeByLevel actualWriteTime;
    
    @Schema(description = "预期用例执行时长（按复杂度等级）")
    private TimeByLevel expectedExecTime;
    
    @Schema(description = "实际用例执行时长（按复杂度等级）")
    private TimeByLevel actualExecTime;
    
    @Schema(description = "用例编写时长偏差率（按复杂度等级）(%)")
    private DeviationByLevel writeTimeDeviationByLevel;
    
    @Schema(description = "用例执行时长偏差率（按复杂度等级）(%)")
    private DeviationByLevel execTimeDeviationByLevel;
    
    // ========== 复用降本指标 (3个) ==========
    @Schema(description = "用例数量复用率 (%)")
    private BigDecimal reuseRateByCount;
    
    @Schema(description = "用例工作量复用率 (%)")
    private BigDecimal reuseRateByWorkload;
    
    @Schema(description = "复用绝对节约工时 (小时)")
    private BigDecimal absoluteTimeSavings;
    
    // ========== 变更热度指标 (2个) ==========
    @Schema(description = "用例新增率 (%)")
    private BigDecimal caseGrowthRate;
    
    @Schema(description = "用例变更热度（被修改用例数）")
    private Integer caseChangeHeat;
    
    // ========== 执行效率指标 (3个) ==========
    @Schema(description = "平均用例执行时长 (分钟)")
    private BigDecimal avgCaseExecDuration;
    
    @Schema(description = "手动用例执行热度 (%)")
    private BigDecimal manualCaseExecHeat;
    
    @Schema(description = "手动用例高执行次数top（JSON字符串）")
    private String topFrequentCasesRaw;

    @Schema(description = "手动用例高执行次数top（解析后）")
    private List<TopFrequentCase> topFrequentCases;

    public String getTopFrequentCasesRaw() {
        return topFrequentCasesRaw;
    }

    public void setTopFrequentCasesRaw(String topFrequentCasesRaw) {
        this.topFrequentCasesRaw = topFrequentCasesRaw;
    }

    public List<TopFrequentCase> getTopFrequentCases() {
        return topFrequentCases;
    }

    public void setTopFrequentCases(List<TopFrequentCase> topFrequentCases) {
        this.topFrequentCases = topFrequentCases;
    }    
    // ========== 额外统计数据 ==========
    @Schema(description = "总用例数")
    private Integer totalCaseCount;
    
    /**
     * 按复杂度等级的时间数据
     */
    @Data
    public static class TimeByLevel {
        @Schema(description = "低复杂度")
        private BigDecimal low;
        
        @Schema(description = "中复杂度")
        private BigDecimal medium;
        
        @Schema(description = "高复杂度")
        private BigDecimal high;
    }
    
    /**
     * 按复杂度等级的偏差率数据
     */
    @Data
    public static class DeviationByLevel {
        @Schema(description = "低复杂度偏差率 (%)")
        private BigDecimal low;
        
        @Schema(description = "中复杂度偏差率 (%)")
        private BigDecimal medium;
        
        @Schema(description = "高复杂度偏差率 (%)")
        private BigDecimal high;
    }
    
    /**
     * 高频执行用例
     */
    @Data
    public static class TopFrequentCase {
        @Schema(description = "用例ID")
        private String caseId;
        
        @Schema(description = "用例名称")
        private String caseName;
        
        @Schema(description = "执行次数")
        private Integer execCount;
        
        @Schema(description = "复杂度")
        private BigDecimal complexity;
    }
}
