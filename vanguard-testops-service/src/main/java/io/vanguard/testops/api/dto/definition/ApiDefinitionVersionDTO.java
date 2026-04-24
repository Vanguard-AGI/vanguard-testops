package io.vanguard.testops.api.dto.definition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class ApiDefinitionVersionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "接口ID")
    private String id;

    @Schema(description = "接口名称")
    private String name;

    @Schema(description = "版本id")
    private String versionId;

    @Schema(description = "版本name")
    private String versionName;

    @Schema(description = "版本引用id")
    private String refId;

    @Schema(description = "项目id")
    private String projectId;

}
