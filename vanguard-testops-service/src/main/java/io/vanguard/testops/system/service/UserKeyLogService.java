package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.UserKey;
import io.vanguard.testops.system.dto.UserKeyDTO;
import io.vanguard.testops.system.dto.builder.LogDTOBuilder;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.mapper.UserKeyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserKeyLogService {

    @Resource
    private UserKeyMapper userKeyMapper;

    public LogDTO deleteLog(String id) {
        UserKey userkey = userKeyMapper.selectByPrimaryKey(id);
        if (userkey != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.DELETE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_APIKEYS)
                    .method(HttpMethodConstants.GET.name())
                    .path("/user/api/key/delete")
                    .sourceId(id)
                    .content(userkey.getAccessKey())
                    .originalValue(JSON.toJSONBytes(userkey))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

    public LogDTO updateLog(UserKeyDTO userKeyDTO) {
        UserKey userkey = userKeyMapper.selectByPrimaryKey(userKeyDTO.getId());
        if (userkey != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_APIKEYS)
                    .method(HttpMethodConstants.POST.name())
                    .path("/user/api/key/update")
                    .sourceId(userkey.getId())
                    .content(userkey.getAccessKey())
                    .originalValue(JSON.toJSONBytes(userkey))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

    public LogDTO enableLog(String id) {
        UserKey userkey = userKeyMapper.selectByPrimaryKey(id);
        if (userkey != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_APIKEYS)
                    .method(HttpMethodConstants.GET.name())
                    .path("/user/api/key/enable")
                    .sourceId(id)
                    .content(userkey.getAccessKey())
                    .originalValue(JSON.toJSONBytes(userkey))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

    public LogDTO disableLog(String id) {
        UserKey userkey = userKeyMapper.selectByPrimaryKey(id);
        if (userkey != null) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(OperationLogConstants.SYSTEM)
                    .organizationId(OperationLogConstants.SYSTEM)
                    .type(OperationLogType.UPDATE.name())
                    .module(OperationLogModule.PERSONAL_INFORMATION_APIKEYS)
                    .method(HttpMethodConstants.GET.name())
                    .path("/user/api/key/disable")
                    .sourceId(id)
                    .content(userkey.getAccessKey())
                    .originalValue(JSON.toJSONBytes(userkey))
                    .build().getLogDTO();
            return dto;
        }
        return null;
    }

}
