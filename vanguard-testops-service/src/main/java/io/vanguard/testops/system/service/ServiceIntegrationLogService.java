package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.ServiceIntegration;
import io.vanguard.testops.system.dto.request.ServiceIntegrationUpdateRequest;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ServiceIntegrationLogService {

    @Resource
    private ServiceIntegrationService serviceIntegrationService;
    @Resource
    private BasePluginService basePluginService;

    public LogDTO addLog(ServiceIntegrationUpdateRequest request) {
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                null,
                null,
                OperationLogType.ADD.name(),
                OperationLogModule.SETTING_ORGANIZATION_SERVICE,
                getName(request.getPluginId()));
        dto.setOriginalValue(JSON.toJSONBytes(request));
        return dto;
    }

    private String getName(String pluginId) {
        return basePluginService.get(pluginId).getName();
    }

    public LogDTO updateLog(ServiceIntegrationUpdateRequest request) {
        ServiceIntegration serviceIntegration = serviceIntegrationService.get(request.getId());
        LogDTO dto = null;
        if (serviceIntegration != null) {
            dto = new LogDTO(
                    OperationLogConstants.ORGANIZATION,
                    null,
                    serviceIntegration.getId(),
                    null,
                    OperationLogType.UPDATE.name(),
                    OperationLogModule.SETTING_ORGANIZATION_SERVICE,
                    getName(serviceIntegration.getPluginId()));
            dto.setOriginalValue(JSON.toJSONBytes(serviceIntegration));
        }
        return dto;
    }

    public LogDTO deleteLog(String id) {
        ServiceIntegration serviceIntegration = serviceIntegrationService.get(id);
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                serviceIntegration.getId(),
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.SETTING_ORGANIZATION_SERVICE,
                getName(serviceIntegration.getPluginId()));
        dto.setOriginalValue(JSON.toJSONBytes(serviceIntegration));
        return dto;
    }
}