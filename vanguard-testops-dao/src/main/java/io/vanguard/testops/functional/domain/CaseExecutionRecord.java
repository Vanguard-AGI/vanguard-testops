package io.vanguard.testops.functional.domain;
import lombok.Data;


import java.io.Serializable;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
* 用例执行记录表（用于统计高频用例和执行时长）
* @TableName case_execution_record
*/
@Data
public class CaseExecutionRecord implements Serializable {

    /**
    * 主键ID
    */
    @Schema(description = "主键ID")
    private String id;
    /**
    * 用例ID
    */
    @Schema(description = "用例ID")
    private String caseId;
    /**
    * 测试计划ID
    */
    @Schema(description = "测试计划ID")
    private String planId;
    /**
    * 项目ID
    */
    @Schema(description = "项目ID")
    private String projectId;
    /**
    * 执行人ID
    */
    @Schema(description = "执行人ID")
    private String executorId;
    /**
    * 执行结果：PASS-通过, FAIL-失败, BLOCKED-阻塞, SKIP-跳过
    */
    @Schema(description = "执行结果：PASS-通过, FAIL-失败, BLOCKED-阻塞, SKIP-跳过")
    private String status;
    /**
    * 执行时长（毫秒）
    */
    @Schema(description = "执行时长（毫秒）")
    private Long duration;
    /**
    * 执行开始时间
    */
    @Schema(description = "执行开始时间")
    private Long startTime;
    /**
    * 执行结束时间
    */
    @Schema(description = "执行结束时间")
    private Long endTime;
    /**
    * 是否首次执行：0-否, 1-是
    */
    @Schema(description = "是否首次执行：0-否, 1-是")
    private Integer isFirstExecution;
    /**
    * 执行时的用例CS分值
    */
    @Schema(description = "执行时的用例CS分值")
    private BigDecimal caseCsScore;
    /**
    * 创建时间
    */
    @Schema(description = "创建时间")
    private Long createTime;
    /**
    * 更新时间
    */
    @Schema(description = "更新时间")
    private Long updateTime;
}
