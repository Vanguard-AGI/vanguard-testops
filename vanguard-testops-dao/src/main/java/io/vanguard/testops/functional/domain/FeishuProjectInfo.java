package io.vanguard.testops.functional.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Data
public class FeishuProjectInfo implements Serializable {
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "飞书项目ID")
    private String feishuProjectId;

    @Schema(description = "飞书项目名称")
    private String feishuProjectName;

    @Schema(description = "关联的内部项目ID")
    private String projectId;

    @Schema(description = "【外部同步】飞书/Jira记录的缺陷数量")
    private Integer defectCount;

    @Schema(description = "【人工预估】飞书上填报的计划编写耗时 (经验值，毫秒)")
    private Long expectedWriteDuration;

    @Schema(description = "最后同步时间")
    private Long lastSyncTime;

    @Schema(description = "同步状态")
    private String syncStatus;

    @Schema(description = "同步错误信息")
    private String syncErrorMsg;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    private static final long serialVersionUID = 1L;
}
