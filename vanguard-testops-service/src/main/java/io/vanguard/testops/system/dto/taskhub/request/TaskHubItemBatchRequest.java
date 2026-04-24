package io.vanguard.testops.system.dto.taskhub.request;

import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Jan
 */
@Data
public class TaskHubItemBatchRequest extends TableBatchProcessDTO {

    @Schema(description = "任务id")
    private String taskId;

    @Schema(description = "资源池id")
    private List<String> resourcePoolIds;

    @Schema(description = "资源池节点")
    private List<String> resourcePoolNodes;

}
