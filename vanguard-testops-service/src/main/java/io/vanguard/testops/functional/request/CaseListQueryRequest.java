package io.vanguard.testops.functional.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用例列表查询请求")
public class CaseListQueryRequest {
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "指标类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "指标类型不能为空")
    private String metricType;
    
    @Schema(description = "维度类型: personal(个人) 或 project(项目)")
    private String dimension;
    
    @Schema(description = "维度值: 用户ID或项目ID，或'all'表示全部")
    private String dimensionValue;
    
    @Schema(description = "开始时间")
    private Long startTime;
    
    @Schema(description = "结束时间")
    private Long endTime;
    
    @Schema(description = "页码，从1开始")
    private Integer pageNum = 1;
    
    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}

