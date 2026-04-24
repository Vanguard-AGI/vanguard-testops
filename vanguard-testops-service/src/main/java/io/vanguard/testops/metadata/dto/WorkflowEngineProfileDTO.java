package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowEngineProfileDTO {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "引擎类型: API/UI")
    private String engineType;

    @Schema(description = "环境: DEV/TEST/PROD")
    private String envCode;

    @Schema(description = "机器人")
    private Map<String, Object> robots;

    @Schema(description = "被测服务数据库及各种数据连接")
    private Map<String, Object> dataEndpoint;

    @Schema(description = "公共参数")
    private Map<String, Object> variables;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "最后修改人")
    private String updateUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "服务域名/IP地址")
    private String domain;

    @Schema(description = "XXL-Job配置信息（JSON格式，含url、账号、规则等）")
    private Map<String, Object> xxljobInfo;

    @Schema(description = "MQ配置信息（JSON格式，含url信息等）")
    private Map<String, Object> mqInfo;

    @Schema(description = "Dubbo调用信息")
    private Map<String, Object> dubboInfo;
}

