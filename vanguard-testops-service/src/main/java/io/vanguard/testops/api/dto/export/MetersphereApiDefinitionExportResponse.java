package io.vanguard.testops.api.dto.export;

import io.vanguard.testops.api.dto.converter.ApiDefinitionExportDetail;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jan
 */
@Data
public class MetersphereApiDefinitionExportResponse extends ApiDefinitionExportResponse {

    private List<ApiDefinitionExportDetail> apiDefinitions = new ArrayList<>();

}
