package io.vanguard.testops.plan.dto;

import io.vanguard.testops.plan.domain.TestPlanCollection;
import lombok.Data;

@Data
public class TestPlanCollectionConfigDTO extends TestPlanCollection {
    private String poolName;

    private String envName;

    private boolean noResourcePool = false;
}
