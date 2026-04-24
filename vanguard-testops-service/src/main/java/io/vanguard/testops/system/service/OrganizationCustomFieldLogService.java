package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.sdk.util.EnumValidator;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.CustomField;
import io.vanguard.testops.system.dto.sdk.request.CustomFieldUpdateRequest;
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
public class OrganizationCustomFieldLogService {

    @Resource
    private OrganizationCustomFieldService organizationCustomFieldService;

    public LogDTO addLog(CustomFieldUpdateRequest request) {
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                null,
                null,
                OperationLogType.ADD.name(),
                getOperationLogModule(request.getScene()),
                request.getName());
        dto.setOriginalValue(JSON.toJSONBytes(request));
        return dto;
    }

    public String getOperationLogModule(String scene) {
        TemplateScene templateScene = EnumValidator.validateEnum(TemplateScene.class, scene);
        switch (templateScene) {
            case API:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_API_FIELD;
            case FUNCTIONAL:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_FUNCTIONAL_FIELD;
            case UI:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_UI_FIELD;
            case BUG:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_BUG_FIELD;
            case TEST_PLAN:
                return OperationLogModule.SETTING_ORGANIZATION_TEMPLATE_TEST_PLAN_FIELD;
            default:
                return null;
        }
    }

    public LogDTO updateLog(CustomFieldUpdateRequest request) {
        CustomField customField = organizationCustomFieldService.getWithCheck(request.getId());
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                customField.getId(),
                null,
                OperationLogType.UPDATE.name(),
                getOperationLogModule(customField.getScene()),
                customField.getName());
        dto.setOriginalValue(JSON.toJSONBytes(customField));
        return dto;
    }

    public LogDTO deleteLog(String id) {
        CustomField customField = organizationCustomFieldService.getWithCheck(id);
        LogDTO dto = new LogDTO(
                OperationLogConstants.ORGANIZATION,
                null,
                customField.getId(),
                null,
                OperationLogType.DELETE.name(),
                getOperationLogModule(customField.getScene()),
                customField.getName());
        dto.setOriginalValue(JSON.toJSONBytes(customField));
        return dto;
    }
}