package io.vanguard.testops.functional.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class MeegoStoryStats implements Serializable {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "需求唯一标识 (Story ID)")
    private String storyId;

    @Schema(description = "需求名称 (Story Name)")
    private String storyName;

    @Schema(description = "Defect类型工作项数量")
    private Integer defectCount;

    @Schema(description = "测分编写预期时间(工时)")
    private Double testAnalysisTime;

    @Schema(description = "更新时间")
    private Date updatedAt;

    private static final long serialVersionUID = 1L;
}