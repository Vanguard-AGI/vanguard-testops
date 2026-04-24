package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.Organization;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserExample;
import io.vanguard.testops.system.dto.request.OrganizationEditRequest;
import io.vanguard.testops.system.dto.request.OrganizationNameEditRequest;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.mapper.OrganizationMapper;
import io.vanguard.testops.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class SystemOrganizationLogService {

	@Resource
	private UserMapper userMapper;
	@Resource
	private OrganizationMapper organizationMapper;
	@Resource
	private OrganizationService organizationService;

	public LogDTO addLog(OrganizationEditRequest organizationEditRequest) {
		LogDTO dto = new LogDTO(
				OperationLogConstants.SYSTEM,
				OperationLogConstants.SYSTEM,
				organizationEditRequest.getId(),
				null,
				OperationLogType.ADD.name(),
				OperationLogModule.SETTING_SYSTEM_ORGANIZATION,
				organizationEditRequest.getName());

		dto.setPath("/system/organization/add");
		dto.setMethod(HttpMethodConstants.POST.name());
		dto.setOriginalValue(JSON.toJSONBytes(organizationEditRequest));
		return dto;
	}

	/**
	 * 更新组织
	 *
	 * @param request 接口请求参数
	 * @return 日志详情
	 */
	public LogDTO updateNameLog(OrganizationNameEditRequest request) {
		Organization organization = organizationMapper.selectByPrimaryKey(request.getId());
		if (organization != null) {
			LogDTO dto = new LogDTO(
					OperationLogConstants.SYSTEM,
					OperationLogConstants.SYSTEM,
					request.getId(),
					null,
					OperationLogType.UPDATE.name(),
					OperationLogModule.SETTING_SYSTEM_ORGANIZATION,
					organization.getName());
			dto.setPath("/system/organization/update");
			dto.setMethod(HttpMethodConstants.POST.name());
			return dto;
		}
		return null;
	}

	/**
	 * 更新组织
	 *
	 * @param request 接口请求参数
	 * @return 日志详情
	 */
	public LogDTO updateLog(OrganizationEditRequest request) {
		Organization organization = organizationMapper.selectByPrimaryKey(request.getId());
		if (organization != null) {
			LogDTO dto = new LogDTO(
					OperationLogConstants.SYSTEM,
					OperationLogConstants.SYSTEM,
					request.getId(),
					null,
					OperationLogType.UPDATE.name(),
					OperationLogModule.SETTING_SYSTEM_ORGANIZATION,
					organization.getName());
			dto.setPath("/system/organization/update");
			dto.setMethod(HttpMethodConstants.POST.name());
			// 新增的组织管理员ID
			List<String> newUserIds = request.getUserIds();
			UserExample example = new UserExample();
			example.createCriteria().andIdIn(newUserIds);
			List<User> newOrgUsers = userMapper.selectByExample(example);
			// 旧的组织管理员ID
			List<String> oldUserIds = organizationService.getOrgAdminIds(request.getId());
			example.clear();
			example.createCriteria().andIdIn(oldUserIds);
			List<User> oldOrgUsers = userMapper.selectByExample(example);
			dto.setOriginalValue(JSON.toJSONBytes(oldOrgUsers));
			dto.setModifiedValue(JSON.toJSONBytes(newOrgUsers));
			return dto;
		}
		return null;
	}


	/**
	 * 删除组织
	 *
	 * @param id 接口请求参数
	 * @return 日志详情
	 */
	public LogDTO deleteLog(String id) {
		Organization organization = organizationMapper.selectByPrimaryKey(id);
		if (organization != null) {
			LogDTO dto = new LogDTO(
					OperationLogConstants.SYSTEM,
					OperationLogConstants.SYSTEM,
					id,
					null,
					OperationLogType.DELETE.name(),
					OperationLogModule.SETTING_SYSTEM_ORGANIZATION,
					organization.getName());
			dto.setPath("/system/organization/delete");
			dto.setMethod(HttpMethodConstants.GET.name());
			dto.setOriginalValue(JSON.toJSONBytes(organization));
			return dto;
		}
		return null;
	}

	/**
	 * 恢复组织
	 *
	 * @param id 接口请求参数
	 * @return 日志详情
	 */
	public LogDTO recoverLog(String id) {
		Organization organization = organizationMapper.selectByPrimaryKey(id);
		if (organization != null) {
			LogDTO dto = new LogDTO(
					OperationLogConstants.SYSTEM,
					OperationLogConstants.SYSTEM,
					id,
					null,
					OperationLogType.RECOVER.name(),
					OperationLogModule.SETTING_SYSTEM_ORGANIZATION,
					organization.getName());
			dto.setPath("/system/organization/recover");
			dto.setMethod(HttpMethodConstants.GET.name());
			dto.setOriginalValue(JSON.toJSONBytes(organization));
			return dto;
		}
		return null;
	}
}
