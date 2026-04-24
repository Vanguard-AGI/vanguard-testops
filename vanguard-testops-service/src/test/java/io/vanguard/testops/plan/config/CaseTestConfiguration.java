package io.vanguard.testops.plan.config;

import io.vanguard.testops.provider.BaseAssociateApiProvider;
import io.vanguard.testops.provider.BaseAssociateBugProvider;
import io.vanguard.testops.provider.BaseAssociateScenarioProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestConfiguration
public class CaseTestConfiguration {

    @MockBean
    BaseAssociateApiProvider provider;

    @MockBean
    BaseAssociateScenarioProvider scenarioProvider;

    @MockBean
    BaseAssociateBugProvider baseAssociateBugProvider;

}
