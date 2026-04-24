package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.sdk.request.GlobalUserRoleRelationUpdateRequest;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import io.vanguard.testops.system.mapper.UserRoleMapper;
import io.vanguard.testops.system.mapper.UserRoleRelationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
public class GlobalUserRoleRelationLogService {

    @Resource
    private UserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private BaseUserMapper baseUserMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * 添加接口日志
     *
     * @param request
     * @return
     */
    public LogDTO addLog(GlobalUserRoleRelationUpdateRequest request) {
        UserRole userRole = userRoleMapper.selectByPrimaryKey(request.getRoleId());
        List<String> userIds = request.getUserIds();
        List<OptionDTO> users = baseUserMapper.selectUserOptionByIds(userIds);
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                userRole.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.SETTING_SYSTEM_USER_GROUP,
                userRole.getName());

        dto.setOriginalValue(JSON.toJSONBytes(users));
        return dto;
    }

    /**
     * 删除接口日志
     *
     * @param id
     * @return
     */
    public LogDTO deleteLog(String id) {
        UserRoleRelation userRoleRelation = userRoleRelationMapper.selectByPrimaryKey(id);
        UserRole userRole = userRoleMapper.selectByPrimaryKey(userRoleRelation.getRoleId());
        LogDTO dto = new LogDTO(
                OperationLogConstants.SYSTEM,
                OperationLogConstants.SYSTEM,
                userRole.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.SETTING_SYSTEM_USER_GROUP,
                userRole.getName());

        UserDTO userDTO = baseUserMapper.selectById(userRoleRelation.getUserId());
        OptionDTO optionDTO = new OptionDTO();
        optionDTO.setId(userDTO.getId());
        optionDTO.setName(userDTO.getName());
        // 记录用户id和name
        dto.setOriginalValue(JSON.toJSONBytes(optionDTO));
        return dto;
    }
}
