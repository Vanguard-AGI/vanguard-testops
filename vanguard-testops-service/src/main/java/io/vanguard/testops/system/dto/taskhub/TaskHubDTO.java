package io.vanguard.testops.system.dto.taskhub;

import io.vanguard.testops.system.domain.ExecTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class TaskHubDTO extends ExecTask {

    @Schema(description = "所属组织")
    private String organizationName;

    @Schema(description = "所属项目")
    private String projectName;

    @Schema(description = "操作人名称")
    private String createUserName;

    @Schema(description = "报告ID")
    private String reportId;

    @Schema(description = "结果是否被删除")
    private Boolean resultDeleted = true;

}
