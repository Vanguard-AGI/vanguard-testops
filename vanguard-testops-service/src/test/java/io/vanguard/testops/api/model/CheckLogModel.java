package io.vanguard.testops.api.model;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */

import io.vanguard.testops.system.log.constants.OperationLogType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckLogModel {
    private String resourceId;
    private OperationLogType operationType;
    private String url;
}
