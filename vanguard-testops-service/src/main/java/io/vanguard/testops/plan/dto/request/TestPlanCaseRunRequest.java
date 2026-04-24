package io.vanguard.testops.plan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @author Jan
 */
@Data
public class TestPlanCaseRunRequest {

    @Schema(description = "项目Id")
    @NotBlank(message = "{test_plan.project_id.not_blank}")
    private String projectId;

    @Schema(description = "id")
    @NotBlank(message = "{id.not_blank}")
    private String id;

    @Schema(description = "测试计划id")
    @NotBlank(message = "{test_plan_id.not_blank}")
    private String testPlanId;

    @Schema(description = "用例id")
    @NotBlank(message = "{case_id.not_blank}")
    private String caseId;

    @Schema(description = "最终执行结果")
    @NotBlank(message = "{test_plan.last_exec_result.not_blank}")
    private String lastExecResult;

    @Schema(description = "步骤执行结果")
    private String stepsExecResult;

    @Schema(description = "执行内容")
    private String content;

    @Schema(description = "评论@的人的Id, 多个以';'隔开")
    private String notifier;

    @Schema(description = "测试计划执行评论富文本的文件id集合")
    private List<String> planCommentFileIds;

    // ==================== 执行耗时追踪字段 (ExecutionTracker埋点) ====================
    
    @Schema(description = "【平台实测】单次实际执行耗时(毫秒) - ExecutionTracker.executionTime")
    private Long actualExecMs;

    @Schema(description = "【平台实测】单次阅读耗时(毫秒) - ExecutionTracker.readingTime")
    private Long actualReadingMs;

    @Schema(description = "是否批量填表 - ExecutionTracker.isBatch")
    private Boolean isBatchFill;

    @Schema(description = "切出次数(窗口失焦次数) - ExecutionTracker.focusOutCount")
    private Integer focusOutCount;

    @Schema(description = "被过滤的无效时长(毫秒) - ExecutionTracker.filteredTime")
    private Long filteredTimeMs;

    @Schema(description = "是否阻塞")
    private Boolean isBlocked;

    @Schema(description = "阻塞原因: ENVIRONMENT/RESOURCE_SHORTAGE/PREREQUISITE_DEPENDENCY/REQUIREMENT_UNCLEAR/TECHNICAL_DIFFICULTY/PROCESS_COMMUNICATION")
    private String blockReason;

}
