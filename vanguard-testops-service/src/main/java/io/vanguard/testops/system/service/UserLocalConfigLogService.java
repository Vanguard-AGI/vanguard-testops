package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.UserLocalConfig;
import io.vanguard.testops.system.dto.UserLocalConfigUpdateRequest;
import io.vanguard.testops.system.dto.builder.LogDTOBuilder;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.mapper.UserLocalConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserLocalConfigLogService {

    @Resource
    private UserLocalConfigMapper userLocalConfigMapper;

    public LogDTO updateLog(UserLocalConfigUpdateRequest request) {
        UserLocalConfig userLocalConfig = userLocalConfigMapper.selectByPrimaryKey(request.getId());
        if (userLocalConfig != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_LOCAL_CONFIG)
                    .method(HttpMethodConstants.POST.name())
                    .path("/user/local/config/update")
                    .sourceId(userLocalConfig.getId())
                    .content(userLocalConfig.getUserUrl())
                    .originalValue(JSON.toJSONBytes(userLocalConfig))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

    public LogDTO enableLog(String id) {
        UserLocalConfig userLocalConfig = userLocalConfigMapper.selectByPrimaryKey(id);
        if (userLocalConfig != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_LOCAL_CONFIG)
                    .method(HttpMethodConstants.GET.name())
                    .path("/user/local/config/enable")
                    .sourceId(id)
                    .content(userLocalConfig.getUserUrl())
                    .originalValue(JSON.toJSONBytes(userLocalConfig))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

    public LogDTO disableLog(String id) {
        UserLocalConfig userLocalConfig = userLocalConfigMapper.selectByPrimaryKey(id);
        if (userLocalConfig != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_LOCAL_CONFIG)
                    .method(HttpMethodConstants.GET.name())
                    .path("/user/local/config/disable")
                    .sourceId(id)
                    .content(userLocalConfig.getUserUrl())
                    .originalValue(JSON.toJSONBytes(userLocalConfig))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

}
