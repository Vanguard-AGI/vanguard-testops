package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowEngineProfileAddRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.project_id.not_blank}")
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.project_id.length_range}")
    private String projectId;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.name.not_blank}")
    @Size(min = 1, max = 255, message = "{workflow_engine_profile.name.length_range}")
    private String name;

    @Schema(description = "引擎类型: API/UI", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.engine_type.not_blank}")
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.engine_type.length_range}")
    private String engineType;

    @Schema(description = "环境: DEV/TEST/PROD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.env_code.not_blank}")
    @Size(min = 1, max = 32, message = "{workflow_engine_profile.env_code.length_range}")
    private String envCode;

    @Schema(description = "机器人")
    private Map<String, Object> robots;

    @Schema(description = "被测服务数据库及各种数据连接")
    private Map<String, Object> dataEndpoint;

    @Schema(description = "公共参数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{workflow_engine_profile.variables.not_blank}")
    private Map<String, Object> variables;

    @Schema(description = "服务域名/IP地址")
    @Size(max = 200, message = "{workflow_engine_profile.domain.length_range}")
    private String domain;

    @Schema(description = "XXL-Job配置信息（JSON格式，含url、账号、规则等）")
    private Map<String, Object> xxljobInfo;

    @Schema(description = "MQ配置信息（JSON格式，含url信息等）")
    private Map<String, Object> mqInfo;

    @Schema(description = "Dubbo调用信息")
    private Map<String, Object> dubboInfo;
}

