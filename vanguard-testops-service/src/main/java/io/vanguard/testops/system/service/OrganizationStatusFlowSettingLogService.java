package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.sdk.util.EnumValidator;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.domain.StatusItem;
import io.vanguard.testops.system.dto.StatusItemDTO;
import io.vanguard.testops.system.dto.sdk.request.StatusDefinitionUpdateRequest;
import io.vanguard.testops.system.dto.sdk.request.StatusFlowUpdateRequest;
import io.vanguard.testops.system.dto.sdk.request.StatusItemAddRequest;
import io.vanguard.testops.system.dto.sdk.request.StatusItemUpdateRequest;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.mapper.StatusItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OrganizationStatusFlowSettingLogService {

    @Resource
    private OrganizationStatusFlowSettingService organizationStatusFlowSettingService;
    @Resource
    private StatusItemMapper statusItemMapper;


    public LogDTO updateStatusDefinitionLog(StatusDefinitionUpdateRequest request) {
        StatusItem statusItem = statusItemMapper.selectByPrimaryKey(request.getStatusId());
        return updateStatusFlowSettingLog(statusItem.getScopeId(), statusItem.getScene());
    }

    public LogDTO addStatusItemLog(StatusItemAddRequest request) {
        return updateStatusFlowSettingLog(request.getScopeId(), request.getScene());
    }

    public LogDTO updateStatusItemLog(StatusItemUpdateRequest request) {
        StatusItem statusItem = statusItemMapper.selectByPrimaryKey(request.getId());
        return updateStatusFlowSettingLog(statusItem.getScopeId(), statusItem.getScene());
    }

    public LogDTO deleteStatusItemLog(String id) {
        StatusItem statusItem = statusItemMapper.selectByPrimaryKey(id);
        return updateStatusFlowSettingLog(statusItem.getScopeId(), statusItem.getScene());
    }

    public LogDTO updateStatusFlowLog(StatusFlowUpdateRequest request) {
        StatusItem statusItem = statusItemMapper.selectByPrimaryKey(request.getFromId());
        return updateStatusFlowSettingLog(statusItem.getScopeId(), statusItem.getScene());
    }

    public LogDTO updateStatusFlowSettingLog(String scopeId, String scene) {
        List<StatusItemDTO> statusFlowSetting = organizationStatusFlowSettingService.getStatusFlowSetting(scopeId, scene);
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                scopeId,
                null,
                OperationLogType.UPDATE.name(),
                getOperationLogModule(scene),
                Translator.get("status_flow.name"));
        dto.setOriginalValue(JSON.toJSONBytes(statusFlowSetting));
        return dto;
    }

    public String getOperationLogModule(String scene) {
        TemplateScene templateScene = EnumValidator.validateEnum(TemplateScene.class, scene);
        switch (templateScene) {
            case BUG:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_BUG_WORKFLOW;
            default:
                return null;
        }
    }
}