package io.vanguard.testops.api.dto.export;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Jan
 */
@Data
public class ApiScenarioExportResponse implements Serializable {

    private String organizationId;

    private String projectId;

    @Serial
    private static final long serialVersionUID = 1L;

}
