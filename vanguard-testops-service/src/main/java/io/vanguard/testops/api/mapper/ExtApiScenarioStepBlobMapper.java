package io.vanguard.testops.api.mapper;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-01-16  19:57
 */
public interface ExtApiScenarioStepBlobMapper {
    List<String> getStepIdsByScenarioId(String scenarioId);
}
