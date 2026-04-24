package io.vanguard.testops.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Jan
 */
@Data
public class ApiImportCurlRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "curl字符串")
    private String curl;
}
