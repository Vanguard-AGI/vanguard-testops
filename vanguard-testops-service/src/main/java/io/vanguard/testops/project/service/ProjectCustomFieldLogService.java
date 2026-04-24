package io.vanguard.testops.project.service;

import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.sdk.util.EnumValidator;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.dto.sdk.request.CustomFieldUpdateRequest;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.CustomField;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectCustomFieldLogService {

    @Resource
    private ProjectCustomFieldService projectCustomFieldService;

    public LogDTO addLog(CustomFieldUpdateRequest request) {
        LogDTO dto = new LogDTO(
                null,
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
                return OperationLogModule.PROJECT_MANAGEMENT_TEMPLATE_API_FIELD;
            case FUNCTIONAL:
                return OperationLogModule.PROJECT_MANAGEMENT_TEMPLATE_FUNCTIONAL_FIELD;
            case UI:
                return OperationLogModule.PROJECT_MANAGEMENT_TEMPLATE_UI_FIELD;
            case BUG:
                return OperationLogModule.PROJECT_MANAGEMENT_TEMPLATE_BUG_FIELD;
            case TEST_PLAN:
                return OperationLogModule.PROJECT_MANAGEMENT_TEMPLATE_TEST_PLAN_FIELD;
            default:
                return null;
        }
    }

    public LogDTO updateLog(CustomFieldUpdateRequest request) {
        CustomField customField = projectCustomFieldService.getWithCheck(request.getId());
        LogDTO dto = new LogDTO(
                null,
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
        CustomField customField = projectCustomFieldService.getWithCheck(id);
        LogDTO dto = new LogDTO(
                null,
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