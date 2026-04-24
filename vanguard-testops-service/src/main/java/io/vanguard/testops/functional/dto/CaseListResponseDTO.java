package io.vanguard.testops.functional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用例列表响应")
public class CaseListResponseDTO {
    
    @Schema(description = "用例列表")
    private List<CaseDetailWithCSDTO> caseList;
    
    @Schema(description = "总记录数")
    private Long total;
    
    @Schema(description = "当前页码")
    private Integer pageNum;
    
    @Schema(description = "每页大小")
    private Integer pageSize;
}

