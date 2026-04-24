package io.vanguard.testops.api.dto.export;

import lombok.Data;

/**
 * @author Jan
 */
@Data
public class SwaggerInfo {
    private String version;
    private String title;
    private String description;
    private String termsOfService;
}
