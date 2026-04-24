package io.vanguard.testops.project.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author Jan
 */

@Getter
@Setter
public class ProjectApplicationRequestDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{project_application.project_id.not_blank}")
    private String projectId;

    @NotBlank(message = "{project_application.type.not_blank}")
    private List<String> types;
}
