package io.vanguard.testops.project.dto.environment.processors;


import com.fasterxml.jackson.annotation.JsonTypeName;
import io.vanguard.testops.project.api.processor.SQLProcessor;
import lombok.Data;

@Data
@JsonTypeName("ENV_SCENARIO_SQL")
public class EnvScenarioSqlProcessor extends SQLProcessor {
}

