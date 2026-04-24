package io.vanguard.testops.api.support.parser.factory;

import io.vanguard.testops.api.constants.ApiImportPlatform;
import io.vanguard.testops.api.support.parser.ApiDefinitionImportParser;
import io.vanguard.testops.api.support.parser.ApiScenarioImportParser;
import io.vanguard.testops.api.support.parser.dataimport.*;
import org.apache.commons.lang3.StringUtils;

public class ImportParserFactory {
    public static ApiDefinitionImportParser<?> getApiDefinitionImportParser(String platform) {
        if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Swagger3.name(), platform)) {
            return new Swagger3ApiDefinitionImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Postman.name(), platform)) {
            return new PostmanApiDefinitionImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.MeterSphere.name(), platform)) {
            return new MetersphereApiDefinitionImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Har.name(), platform)) {
            return new HarApiDefinitionImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Jmeter.name(), platform)) {
            return new JmeterApiDefinitionImportParser();
        }
        return null;
    }

    public static ApiScenarioImportParser getApiScenarioImportParser(String platform) {
        if (StringUtils.equalsIgnoreCase(ApiImportPlatform.MeterSphere.name(), platform)) {
            return new MetersphereApiScenarioImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Jmeter.name(), platform)) {
            return new JmeterApiScenarioImportParser();
        } else if (StringUtils.equalsIgnoreCase(ApiImportPlatform.Har.name(), platform)) {
            return new HarApiScenarioImportParser();
        }
        return null;
    }
}
