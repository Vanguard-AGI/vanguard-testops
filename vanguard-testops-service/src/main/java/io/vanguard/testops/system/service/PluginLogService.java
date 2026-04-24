package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.Plugin;
import io.vanguard.testops.system.dto.request.PluginUpdateRequest;
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
public class PluginLogService {

    @Resource
    private PluginService pluginService;

    public LogDTO addLog(PluginUpdateRequest request) {
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                null,
                null,
                OperationLogType.ADD.name(),
                OperationLogModule.SETTING_SYSTEM_PLUGIN_MANAGEMENT,
                request.getName());
        dto.setOriginalValue(JSON.toJSONBytes(request));
        return dto;
    }

    public LogDTO updateLog(PluginUpdateRequest request) {
        Plugin plugin = pluginService.get(request.getId());
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                plugin.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.SETTING_SYSTEM_PLUGIN_MANAGEMENT,
                plugin.getName());
        dto.setOriginalValue(JSON.toJSONBytes(plugin));
        return dto;
    }

    public LogDTO deleteLog(String id) {
        Plugin plugin = pluginService.get(id);
        if (plugin == null) {
            return null;
        }
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                plugin.getId(),
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.SETTING_SYSTEM_PLUGIN_MANAGEMENT,
                plugin.getName());
        dto.setOriginalValue(JSON.toJSONBytes(plugin));
        return dto;
    }
}