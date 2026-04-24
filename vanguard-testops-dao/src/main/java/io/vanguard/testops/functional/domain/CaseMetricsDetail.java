package io.vanguard.testops.functional.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CaseMetricsDetail implements Serializable {
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "业务用例ID")
    private String caseId;

    @Schema(description = "来源用例ID(与functional_case.source_case_id一致，复制/导入时从业务表同步，用于执行归父)")
    private String sourceCaseId;

    @Schema(description = "复用类型(与functional_case.reuse_type一致，从业务表同步): DIRECT_REUSE/ADAPT_REUSE")
    private String reuseType;

    @Schema(description = "归属项目ID")
    private String projectId;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "C1:认知/风险(P0/P1) 权重0.5")
    private BigDecimal csFactorC1;

    @Schema(description = "C2:前置条件数量 权重1.5")
    private BigDecimal csFactorC2;

    @Schema(description = "C3:数据准备难度 权重4.0")
    private BigDecimal csFactorC3;

    @Schema(description = "C4:步骤细节数 权重1.0")
    private BigDecimal csFactorC4;

    @Schema(description = "C5:验证点数量 权重2.0")
    private BigDecimal csFactorC5;

    @Schema(description = "C6:逻辑分支数量 权重3.0")
    private BigDecimal csFactorC6;

    @Schema(description = "CS综合分值")
    private BigDecimal csScore;

    @Schema(description = "复杂度等级: L1/L2/L3/L4")
    private String complexityLevel;

    @Schema(description = "用例类型: MANUAL(系数1.0)/AUTO(系数1.5)")
    private String caseType;

    @Schema(description = "环境稳定性因子: 1.0(稳定)/1.5(不稳定-遇到环境阻塞)")
    private BigDecimal envFactor;

    @Schema(description = "【算法理论】基于CS计算的编写工时 (T) = 基准工时 × case_type因子 × env_factor")
    private Long algoExpectedWriteMs;

    @Schema(description = "【算法理论】基于CS计算的执行工时 (E) = 基准工时 × case_type因子 × env_factor")
    private Long algoExpectedExecMs;

    @Schema(description = "【平台实测】前端埋点统计的真实编写耗时")
    private Long platformActualWriteMs;

    @Schema(description = "用例来源: NEW(新建)/REUSE(复用自其他用例)/COPY(复制后修改)")
    private String caseSourceType;

    @Schema(description = "【平台实测】复用后的修改耗时(毫秒) - 前端 ModificationTracker 埋点")
    private Long modificationCostMs;

    @Schema(description = "节约工时(毫秒) = algo_expected_write_ms - modification_cost_ms")
    private Long savedWriteMs;

    @Schema(description = "UQS质量评分(0-100)")
    private BigDecimal uqsScore;

    @Schema(description = "被复用次数")
    private Integer reuseCount;

    @Schema(description = "核心内容变更次数")
    private Integer modifyCount;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "指标最后重算时间")
    private Long lastCalcTime;

    private static final long serialVersionUID = 1L;
}
