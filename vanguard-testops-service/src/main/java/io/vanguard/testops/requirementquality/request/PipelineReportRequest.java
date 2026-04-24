package io.vanguard.testops.requirementquality.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 云效流水线白名单上报请求（运维在流水线中通过 curl 上报）
 * 本接口接收 repoName、serviceName、endpointType、pipelineId、pipelineName、deployTime、locAdd、locDelete、deployer、details；其余字段均为人工在界面操作落库。
 */
@Data
public class PipelineReportRequest {

    @NotBlank(message = "代码仓库名称不能为空")
    @Schema(description = "代码仓库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repoName;

    @Schema(description = "服务名称 / 应用名称，可为空")
    private String serviceName;

    @Schema(description = "其它信息，可为空；支持字符串或 JSON 对象（如流水线 env），落库时序列化为字符串")
    private Object otherInfo;

    @NotBlank(message = "前端|后端不能为空")
    @Schema(description = "FRONTEND / BACKEND / MIXED，或中文 前端/后端/混合", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endpointType;

    @NotBlank(message = "流水线ID不能为空")
    @Schema(description = "流水线运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pipelineId;

    @Schema(description = "流水线名称")
    private String pipelineName;

    @NotBlank(message = "发布时间不能为空")
    @Schema(description = "发布时间（毫秒时间戳），支持数字或数字字符串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deployTime;

    @Schema(description = "新增行数，支持数字或数字字符串，默认 0")
    private String locAdd = "0";

    @Schema(description = "删除行数，支持数字或数字字符串，默认 0")
    private String locDelete = "0";

    @Schema(description = "发布人")
    private String deployer;

    @Schema(description = "明细列表，支持 JSON 数组或数组字符串")
    private Object details;
}
