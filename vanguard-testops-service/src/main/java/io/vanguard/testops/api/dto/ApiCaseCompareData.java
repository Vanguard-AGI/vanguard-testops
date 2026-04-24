package io.vanguard.testops.api.dto;

import io.vanguard.testops.plugin.api.spi.MsTestElement;
import lombok.Data;

/**
 * @Author: Jan
 * @CreateTime: 2024-08-02  17:49
 */
@Data
public class ApiCaseCompareData {
    private MsTestElement apiRequest;
    private MsTestElement caseRequest;
}
