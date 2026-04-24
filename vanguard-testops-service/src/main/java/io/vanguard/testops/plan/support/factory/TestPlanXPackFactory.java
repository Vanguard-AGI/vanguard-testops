package io.vanguard.testops.plan.support.factory;

import io.vanguard.testops.plan.service.TestPlanGroupService;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.system.dto.sdk.LicenseDTO;
import io.vanguard.testops.system.service.LicenseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class TestPlanXPackFactory {

    private TestPlanGroupService testPlanGroupService;
    private LicenseService licenseService;

    public TestPlanGroupService getTestPlanGroupService() {
        this.checkService();
        // 临时移除许可证检查，允许所有功能
        // if (licenseValidate()) {
        //     return testPlanGroupService;
        // } else {
        //     return null;
        // }
        return testPlanGroupService;
    }

    private void checkService() {
        if (licenseService == null) {
            licenseService = CommonBeanFactory.getBean(LicenseService.class);
        }
        if (testPlanGroupService == null) {
            testPlanGroupService = CommonBeanFactory.getBean(TestPlanGroupService.class);
        }
    }

    private boolean licenseValidate() {
        // 临时移除许可证检查，允许所有功能
        return true;
        // LicenseDTO licenseDTO = licenseService.validate();
        // return (licenseDTO != null && StringUtils.equals(licenseDTO.getStatus(), "valid"));
    }
}
