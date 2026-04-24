package io.vanguard.testops.plan.dto.response;

import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Jan
 */
@Data
public class TestPlanCaseExecHistoryResponse extends TestPlanCaseExecuteHistory {

    @Schema(description = "评审解析内容")
    private String contentText;

    @Schema(description = "步骤结果")
    private String stepsExecResult;

    @Schema(description = "执行人姓名")
    private String userName;

    @Schema(description = "执行人头像")
    private String userLogo;

    @Schema(description = "执行人邮箱")
    private String email;

    @Schema(description = "编辑模式")
    private String caseEditType;

    @Schema(description = "是否显示步骤信息")
    private boolean showResult = false;
}
