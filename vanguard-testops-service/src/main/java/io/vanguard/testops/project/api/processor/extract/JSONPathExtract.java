package io.vanguard.testops.project.api.processor.extract;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

/**
 * JSONPath提取
 */
@Data
@JsonTypeName("JSON_PATH")
public class JSONPathExtract extends ResultMatchingExtract {
}
