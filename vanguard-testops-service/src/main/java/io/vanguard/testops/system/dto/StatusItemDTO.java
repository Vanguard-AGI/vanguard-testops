package io.vanguard.testops.system.dto;

import io.vanguard.testops.system.domain.StatusItem;
import lombok.Data;

import java.util.List;


/**
 * @author Jan
 */
@Data
public class StatusItemDTO extends StatusItem {

    private List<String> statusDefinitions;
    private List<String> statusFlowTargets;
}
