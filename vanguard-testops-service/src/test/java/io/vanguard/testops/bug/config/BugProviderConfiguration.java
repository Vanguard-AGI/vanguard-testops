package io.vanguard.testops.bug.config;

import io.vanguard.testops.plugin.platform.spi.Platform;
import io.vanguard.testops.provider.BaseAssociateCaseProvider;
import io.vanguard.testops.system.service.LicenseService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestConfiguration
public class BugProviderConfiguration {

    @MockBean
    BaseAssociateCaseProvider baseAssociateCaseProvider;

    @MockBean
    LicenseService licenseService;

    @MockBean
    Platform platform;
}
