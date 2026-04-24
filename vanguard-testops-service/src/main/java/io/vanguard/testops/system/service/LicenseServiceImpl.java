package io.vanguard.testops.system.service;

import io.vanguard.testops.system.dto.sdk.LicenseDTO;
import org.springframework.stereotype.Service;

/**
 * 临时的LicenseService实现，绕过所有许可证检查
 */
@Service
public class LicenseServiceImpl implements LicenseService {

    @Override
    public LicenseDTO refreshLicense() {
        return getDefaultLicense();
    }

    @Override
    public LicenseDTO validate() {
        return getDefaultLicense();
    }

    @Override
    public LicenseDTO addLicense(String licenseCode, String userId) {
        return getDefaultLicense();
    }

    @Override
    public String getCode(String encrypt) {
        return "ENTERPRISE_LICENSE_ENABLED";
    }

    @Override
    public LicenseDTO getDefaultLicense() {
        LicenseDTO licenseDTO = new LicenseDTO();
        licenseDTO.setStatus("valid");
        // LicenseDTO没有setMessage方法，移除这行
        // licenseDTO.setMessage("Enterprise features enabled");
        return licenseDTO;
    }
}
