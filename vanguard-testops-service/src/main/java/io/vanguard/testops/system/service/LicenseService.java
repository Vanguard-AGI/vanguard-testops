package io.vanguard.testops.system.service;


import io.vanguard.testops.system.dto.sdk.LicenseDTO;

public interface LicenseService {

    LicenseDTO refreshLicense();

    LicenseDTO validate();

    LicenseDTO addLicense(String licenseCode, String userId);

    String getCode(String encrypt);

    /**
     * 临时默认实现，绕过许可证检查
     */
    default LicenseDTO getDefaultLicense() {
        LicenseDTO licenseDTO = new LicenseDTO();
        licenseDTO.setStatus("valid");
        // LicenseDTO没有setMessage方法，移除这行
        // licenseDTO.setMessage("Enterprise features enabled");
        return licenseDTO;
    }
}
