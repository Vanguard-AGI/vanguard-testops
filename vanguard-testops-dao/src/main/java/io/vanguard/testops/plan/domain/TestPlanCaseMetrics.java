package io.vanguard.testops.plan.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TestPlanCaseMetrics implements Serializable {
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "测试计划ID")
    private String testPlanId;

    @Schema(description = "用例ID")
    private String caseId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "来源: NEW(新建)/REUSE(复用)/MODIFY(复用修改)")
    private String caseSourceType;

    @Schema(description = "复用后的修改耗时")
    private Long modificationCostMs;

    @Schema(description = "节约工时 = 理论T - 修改耗时")
    private Long savedWriteMs;

    @Schema(description = "本计划内执行次数")
    private Integer execCount;

    @Schema(description = "首次执行时间")
    private Long firstExecTime;

    @Schema(description = "首次执行结果(关键): PASS/FAIL/BLOCK")
    private String firstExecResult;

    @Schema(description = "最终结果")
    private String lastExecResult;

    @Schema(description = "是否阻塞(影响可执行率)")
    private Boolean isBlockedRun;

    @Schema(description = "阻塞原因: ENVIRONMENT/RESOURCE_SHORTAGE/PREREQUISITE_DEPENDENCY/REQUIREMENT_UNCLEAR/TECHNICAL_DIFFICULTY/PROCESS_COMMUNICATION")
    private String blockReason;

    @Schema(description = "快照CS分值")
    private BigDecimal snapshotCsScore;

    @Schema(description = "快照复杂度等级")
    private String snapshotLevel;

    // ==================== 执行耗时追踪字段 (ExecutionTracker埋点) ====================
    
    @Schema(description = "【平台实测】单次实际执行耗时(毫秒) - ExecutionTracker.executionTime")
    private Long actualExecMs;

    @Schema(description = "【平台实测】单次阅读耗时(毫秒) - ExecutionTracker.readingTime")
    private Long actualReadingMs;

    @Schema(description = "是否批量填表 - ExecutionTracker.isBatch")
    private Integer isBatchFill;

    @Schema(description = "切出次数(窗口失焦次数) - ExecutionTracker.focusOutCount")
    private Integer focusOutCount;

    @Schema(description = "被过滤的无效时长(毫秒) - ExecutionTracker.filteredTime")
    private Long filteredTimeMs;

    @Schema(description = "【平台实测】总耗时(毫秒) = actual_exec_ms + actual_reading_ms")
    private Long totalTimeMs;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    private static final long serialVersionUID = 1L;
}
