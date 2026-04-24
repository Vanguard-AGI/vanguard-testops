package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 用例执行明细行 - Mapper 查询结果（原始聚合字段，由 Service 转为 DTO）
 */
@Data
public class RequirementQualityCaseExecutionRowVO {
    private String planId;
    private String caseId;
    private String caseName;
    private Long execCountSum;
    private Long totalTimeMs;
    /** 单次执行最大耗时(毫秒)，来自 case_execution_record */
    private Long maxDurationMs;
    /** 执行记录总耗时(毫秒)，来自 case_execution_record sum(exec_duration)，用于与 max 同源算平均 */
    private Long totalDurationMsFromCer;
    /** 执行记录中有耗时的条数（exec_duration > 0），用于平均时分母，避免批量 0 拉低 */
    private Long durationPositiveCount;
    /** 成功次数（来自 case_execution_record 每条执行的 exec_result） */
    private Long successCountRecord;
    /** 失败次数（来自 case_execution_record 每条执行的 exec_result） */
    private Long failCountRecord;
    private String lastExecResultMetrics;
    private String lastExecResultTpfc;
}
