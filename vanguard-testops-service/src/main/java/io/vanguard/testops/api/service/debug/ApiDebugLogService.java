package io.vanguard.testops.api.service.debug;

import io.vanguard.testops.api.domain.ApiDebug;
import io.vanguard.testops.api.dto.debug.ApiDebugAddRequest;
import io.vanguard.testops.api.dto.debug.ApiDebugUpdateRequest;
import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
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
public class ApiDebugLogService {

    @Resource
    private ApiDebugService apiDebugService;

    public LogDTO addLog(ApiDebugAddRequest request) {
        LogDTO dto = new LogDTO(
                request.getProjectId(),
                null,
                null,
                null,
                OperationLogType.ADD.name(),
                OperationLogModule.API_TEST_DEBUG_MANAGEMENT_DEBUG,
                request.getName());
        dto.setOriginalValue(JSON.toJSONBytes(request));
        return dto;
    }

    public LogDTO updateLog(ApiDebugUpdateRequest request) {
        ApiDebug apiDebug = apiDebugService.get(request.getId());
        LogDTO dto = null;
        if (apiDebug != null) {
            dto = new LogDTO(
                    apiDebug.getProjectId(),
                    null,
                    apiDebug.getId(),
                    null,
                    OperationLogType.UPDATE.name(),
                    OperationLogModule.API_TEST_DEBUG_MANAGEMENT_DEBUG,
                    apiDebug.getName());
            dto.setOriginalValue(JSON.toJSONBytes(apiDebug));
        }
        return dto;
    }

    public LogDTO deleteLog(String id) {
        ApiDebug apiDebug = apiDebugService.get(id);
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                apiDebug.getId(),
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.API_TEST_DEBUG_MANAGEMENT_DEBUG,
                apiDebug.getName());
        dto.setOriginalValue(JSON.toJSONBytes(apiDebug));
        return dto;
    }

    public LogDTO moveLog(String id) {
        ApiDebug apiDebug = apiDebugService.get(id);
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                apiDebug.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.API_TEST_DEBUG_MANAGEMENT_DEBUG,
                apiDebug.getName());
        dto.setOriginalValue(JSON.toJSONBytes(apiDebug));
        return dto;
    }
}