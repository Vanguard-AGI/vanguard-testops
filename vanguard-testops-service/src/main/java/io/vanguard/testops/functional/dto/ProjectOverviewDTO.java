package io.vanguard.testops.functional.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目概览指标DTO（21个核心指标）
 * 对应前端 ProjectOverviewMetrics 接口
 */
@Data
public class ProjectOverviewDTO {
    private String projectId;

    // ========== UQS质量指标 (2个) ==========
    private BigDecimal avgUQS = BigDecimal.ZERO;                    // 用例质量综合评分（UQS）(0-100)
    private BigDecimal firstPassRate = BigDecimal.ZERO;             // 测试计划首次通过率 (%)
    
    // ========== UQS子指标（用于前端展示）==========
    private BigDecimal defectDiscoveryRate = BigDecimal.ZERO;       // 缺陷发现率 (%)
    private BigDecimal executableRate = BigDecimal.ZERO;            // 可执行率 (%)
    private BigDecimal reuseExecutionRate = BigDecimal.ZERO;        // 复用率 (%)

    // ========== 复杂度指标 (4个) ==========
    private BigDecimal totalWriteComplexity = BigDecimal.ZERO;      // 用例编写总复杂分
    private BigDecimal totalExecComplexity = BigDecimal.ZERO;       // 用例执行总复杂分
    private BigDecimal avgComplexity = BigDecimal.ZERO;             // 用例平均复杂度
    private BigDecimal complexityVariance = BigDecimal.ZERO;        // 用例复杂度方差

    // ========== 工时指标 - 整体偏差 (2个) ==========
    private BigDecimal avgWriteTimeDeviation = BigDecimal.ZERO;     // 平均编写工时偏差率 (%)
    private BigDecimal avgExecTimeDeviation = BigDecimal.ZERO;      // 平均执行工时偏差率 (%)

    // ========== 工时指标 - 按复杂度分级 (6个) ==========
    private TimeByLevel expectedWriteTime = new TimeByLevel();      // 预期用例编写时长（按复杂度等级）(小时)
    private TimeByLevel actualWriteTime = new TimeByLevel();        // 实际用例编写时长（按复杂度等级）(小时)
    private TimeByLevel expectedExecTime = new TimeByLevel();       // 预期用例执行时长（按复杂度等级）(分钟)
    private TimeByLevel actualExecTime = new TimeByLevel();         // 实际用例执行时长（按复杂度等级）(分钟)
    private TimeByLevel writeTimeDeviationByLevel = new TimeByLevel(); // 用例编写时长偏差率（按复杂度等级）(%)
    private TimeByLevel execTimeDeviationByLevel = new TimeByLevel();  // 用例执行时长偏差率（按复杂度等级）(%)

    // ========== 复用降本指标 (3个) ==========
    private BigDecimal reuseRateByCount = BigDecimal.ZERO;          // 用例数量复用率 (%)
    private BigDecimal reuseRateByWorkload = BigDecimal.ZERO;       // 用例工作量复用率 (%)
    private BigDecimal absoluteTimeSavings = BigDecimal.ZERO;       // 复用绝对节约工时 (小时)

    // ========== 变更热度指标 (2个) ==========
    private BigDecimal caseGrowthRate = BigDecimal.ZERO;            // 用例新增率 (%)
    private BigDecimal caseChangeHeat = BigDecimal.ZERO;            // 用例变更热度 (%)

    // ========== 执行效率指标 (3个) ==========
    private BigDecimal avgCaseExecDuration = BigDecimal.ZERO;       // 平均用例执行时长 (分钟)
    private BigDecimal manualCaseExecHeat = BigDecimal.ZERO;        // 手动用例执行热度 (%)
    private List<TopFrequentCase> topFrequentCases = new ArrayList<>(); // 手动用例高执行次数top

    // ========== 额外统计数据 ==========
    private Integer totalCaseCount = 0;                             // 总用例数：按项目查询项目下所有未删除的用例数量
    private Integer effectiveCaseCount = 0;                         // 有效用例数：单位时间内两库中的用例新增数量（按 create_time）

    // 用例数量复用率细分：复用用例 = 从两库导入导出的 + 直接复制两库的；复用用例数 = 直接复用数 + 适配复用数
    private Integer reusedCaseCount = 0;                            // 复用用例数（= 直接 + 适配）
    private Integer directReuseCount = 0;                           // 直接复用数（仅改标题）
    private Integer adaptReuseCount = 0;                            // 适配复用数（改了步骤/预期等）
    /** 复用指标用总用例数 = 用例模板库 + 回归用例库 + 最近2周新增（与用例数量复用率分母一致，仅在复用卡片展示） */
    private Integer totalCaseCountForReuse = 0;

    // ========== 分子分母数据（用于前端展示计算公式验证）==========
    
    // 用例新增率的分子分母
    private Long newCaseCount = 0L;                                 // 单位时间内创建的用例数（新增率分子）
    private Long periodStartCaseCount = 0L;                         // 期初用例数（分母）
    
    // 平均用例执行时长的分子分母
    private Long totalExecDurationMs = 0L;                          // 执行时长总和（毫秒，分子）
    private Long totalExecCount = 0L;                               // 执行次数（分母）
    
    // 手动用例执行热度的分子分母
    private BigDecimal highFreqCsTotal = BigDecimal.ZERO;           // 高频回归用例CS总分（分子）
    private BigDecimal allExecCsTotal = BigDecimal.ZERO;            // 所有执行用例CS总分（分母）
    
    // 首次通过率的分子分母
    private Long firstPassCount = 0L;                               // 首次执行通过用例数（分子）
    private Long firstExecCount = 0L;                               // 首次执行总用例数（分母）
    
    // 编写工时偏差率的分子分母
    private BigDecimal actualWriteDurationHours = BigDecimal.ZERO;  // 实际编写工时（小时，分子）
    private BigDecimal expectedWriteDurationHours = BigDecimal.ZERO;// 理论编写工时（小时，分母）
    
    // 执行工时偏差率的分子分母
    private BigDecimal actualExecDurationMinutes = BigDecimal.ZERO; // 实际执行工时（分钟，分子）
    private BigDecimal expectedExecDurationMinutes = BigDecimal.ZERO;// 理论执行工时（分钟，分母）
    
    // 用例工作量复用率的分子分母
    private BigDecimal reusedCsTotal = BigDecimal.ZERO;             // 复用用例总CS分值（分子）
    private BigDecimal totalCsScore = BigDecimal.ZERO;              // 总CS分值（分母）
    
    // 用例变更率的分子分母（分子=变更原因不为 COPY 的用例数，分母=项目下总用例数，均不限时间）
    private Long modifiedCaseCount = 0L;                            // 变更原因非 COPY 的用例数量（分子）
    private Long totalCaseCountInPeriod = 0L;                       // 项目下总用例数（分母）

    /**
     * 按复杂度等级的时间数据
     */
    @Data
    public static class TimeByLevel {
        private BigDecimal l1 = BigDecimal.ZERO;                    // L1复杂度
        private BigDecimal l2 = BigDecimal.ZERO;                    // L2复杂度
        private BigDecimal l3 = BigDecimal.ZERO;                    // L3复杂度
        private BigDecimal l4 = BigDecimal.ZERO;                    // L4复杂度
    }

    /**
     * 高频执行用例
     */
    @Data
    public static class TopFrequentCase {
        private String caseId;                                      // 用例ID
        private String caseName;                                    // 用例名称
        private Integer execCount;                                  // 执行次数
        private BigDecimal complexity;                              // 复杂度
    }
}
