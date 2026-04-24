package io.vanguard.testops.functional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "用例详情及CS值")
public class CaseDetailWithCSDTO {
    
    @Schema(description = "用例ID")
    private String caseId;
    
    @Schema(description = "用例编号")
    private String caseNum;
    
    @Schema(description = "用例名称")
    private String caseName;
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "模块ID")
    private String moduleId;
    
    @Schema(description = "模块名称")
    private String moduleName;
    
    @Schema(description = "创建人")
    private String createUser;
    
    @Schema(description = "创建人名称")
    private String createUserName;
    
    @Schema(description = "创建时间")
    private Long createTime;
    
    @Schema(description = "更新时间")
    private Long updateTime;
    
    @Schema(description = "CS总分")
    private BigDecimal csScore;
    
    @Schema(description = "认知复杂度得分")
    private BigDecimal cognitiveScore;
    
    @Schema(description = "前置条件复杂度得分")
    private BigDecimal preconditionScore;
    
    @Schema(description = "步骤细节复杂度得分")
    private BigDecimal stepDetailScore;
    
    @Schema(description = "C1：标题长度得分")
    private BigDecimal csFactorC1;
    
    @Schema(description = "C2：风险等级得分")
    private BigDecimal csFactorC2;
    
    @Schema(description = "C3：前置条件数量")
    private BigDecimal csFactorC3;
    
    @Schema(description = "C4：复杂数据准备")
    private BigDecimal csFactorC4;
    
    @Schema(description = "C5：操作步骤数")
    private BigDecimal csFactorC5;
    
    @Schema(description = "C6：验证点数")
    private BigDecimal csFactorC6;
    
    @Schema(description = "C7：逻辑分支数")
    private BigDecimal csFactorC7;
    
    @Schema(description = "用例描述")
    private String description;
    
    @Schema(description = "前置条件")
    private String prerequisite;
}

