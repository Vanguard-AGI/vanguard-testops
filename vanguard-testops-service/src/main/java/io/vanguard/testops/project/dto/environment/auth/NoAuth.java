package io.vanguard.testops.project.dto.environment.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

/**
 * @Author: Jan
 * @CreateTime: 2023-11-07  11:00
 */
@Data
@JsonTypeName("NONE")
public class NoAuth extends HTTPAuthConfig {
}
