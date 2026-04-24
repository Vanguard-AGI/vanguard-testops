package io.vanguard.testops.api.support.parser;

import io.vanguard.testops.api.dto.definition.ApiDefinitionWithBlob;
import io.vanguard.testops.project.domain.Project;

import java.util.List;
import java.util.Map;

public interface ExportParser<T> {
    T parse(List<ApiDefinitionWithBlob> list, Project project, Map<String, String> moduleMap) throws Exception;
}
