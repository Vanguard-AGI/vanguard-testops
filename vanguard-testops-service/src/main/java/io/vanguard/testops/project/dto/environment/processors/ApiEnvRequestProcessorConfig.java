package io.vanguard.testops.project.dto.environment.processors;

import io.vanguard.testops.project.api.processor.MsProcessor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 接口测试-请求级前后置配置
 * @Author: Jan
 * @CreateTime: 2024-02-01  14:53
 */
@Data
public class ApiEnvRequestProcessorConfig  implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 预留其他配置

    @Schema(description = "前后置列表")
    private List<MsProcessor> processors = new ArrayList<>(0);
}
