package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 公共节点分页查询请求 DTO
 * 支持单项目 projectId 或多项目 projectIds，二者至少传其一；projectIds 优先。
 */
@Data
public class WorkflowPublicNodePageRequest {

    @Schema(description = "项目ID（与 projectIds 二选一，projectIds 优先）")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @Schema(description = "项目ID列表（多选时使用，与 projectId 二选一，优先使用）")
    private List<@Size(min = 1, max = 50) String> projectIds;

    @Schema(description = "节点分类: api/data/logic/script/other（可选，不传则查询所有分类）")
    @Size(max = 20, message = "节点分类长度不能超过20")
    private String category;

    @Schema(description = "搜索关键词（节点名称或描述）")
    @Size(max = 255, message = "搜索关键词长度不能超过255")
    private String keyword;

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页数量", example = "100")
    private Integer pageSize = 100;
}

