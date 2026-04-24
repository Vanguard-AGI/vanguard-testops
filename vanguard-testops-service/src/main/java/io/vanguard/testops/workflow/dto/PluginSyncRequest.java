package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 插件同步请求 DTO
 * 用于接收外部插件同步的节点数据
 */
@Data
public class PluginSyncRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户邮箱不能为空")
    private String email;

    @Schema(description = "节点列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "节点列表不能为空")
    private List<EndpointData> endpoints;

    /**
     * 节点数据
     */
    @Data
    public static class EndpointData implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "节点类型: HTTP/SQL/DUBBO/ROCKETMQ", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "节点类型不能为空")
        private String type;

        // HTTP 类型字段
        @Schema(description = "HTTP 方法 (GET/POST/PUT/DELETE等)")
        private String method;

        @Schema(description = "HTTP URL")
        private String url;

        @Schema(description = "HTTP 路径")
        private String path;

        @Schema(description = "HTTP 请求头")
        private Map<String, String> headers;

        @Schema(description = "HTTP 查询参数")
        private Map<String, String> queryParams;

        @Schema(description = "HTTP JSON Body")
        private Object json;

        @Schema(description = "HTTP 表单数据")
        private Map<String, Object> data;

        @Schema(description = "HTTP 原始Body")
        private Object body;

        // SQL 类型字段
        @Schema(description = "SQL 语句")
        private String sql;

        @Schema(description = "SQL 操作类型 (SELECT/INSERT/UPDATE/DELETE等)")
        private String operationType;

        // DUBBO 类型字段
        @Schema(description = "DUBBO 接口名称")
        private String interfaceName;

        @Schema(description = "DUBBO 方法名称")
        private String methodName;

        @Schema(description = "DUBBO 参数列表")
        private List<Object> params;

        @Schema(description = "DUBBO 参数类型列表")
        private List<String> paramTypes;

        @Schema(description = "DUBBO 分组")
        private String group;

        @Schema(description = "DUBBO 版本")
        private String version;

        @Schema(description = "DUBBO 超时时间")
        private Integer timeout;

        @Schema(description = "DUBBO 注册中心URL")
        private String dubboUrl;

        @Schema(description = "DUBBO 应用名称")
        private String applicationName;

        @Schema(description = "DUBBO Tag（分支环境标签）")
        private String dubboTag;

        // ROCKETMQ 类型字段
        @Schema(description = "ROCKETMQ Topic")
        private String topic;

        @Schema(description = "ROCKETMQ Tag")
        private String tag;

        @Schema(description = "ROCKETMQ Key")
        private String key;

        @Schema(description = "ROCKETMQ MessageKey (messageKey的别名)")
        private String messageKey;

        @Schema(description = "ROCKETMQ 消息体")
        private String messageBody;

        @Schema(description = "ROCKETMQ URL")
        private String mqUrl;

        @Schema(description = "ROCKETMQ NameServer (nameServer的别名)")
        private String nameServer;

        // 公共字段（DUBBO 和 ROCKETMQ 都可能使用）
        @Schema(description = "SiteTenant (DUBBO 和 ROCKETMQ 共用)")
        private String siteTenant;

        // 其他可能的字段
        @Schema(description = "其他扩展字段")
        private Map<String, Object> extra;
    }
}

