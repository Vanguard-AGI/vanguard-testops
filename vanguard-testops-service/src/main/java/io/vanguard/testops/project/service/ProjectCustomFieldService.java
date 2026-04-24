package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.sdk.constants.TemplateScopeType;
import io.vanguard.testops.system.dto.sdk.CustomFieldDTO;
import io.vanguard.testops.system.dto.sdk.request.CustomFieldOptionRequest;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.domain.CustomField;
import io.vanguard.testops.system.service.BaseCustomFieldService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.vanguard.testops.project.enums.result.ProjectResultCode.PROJECT_TEMPLATE_PERMISSION;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectCustomFieldService extends BaseCustomFieldService {

    @Resource
    private ProjectService projectService;

    @Override
    public List<CustomFieldDTO> list(String projectId, String scene) {
        projectService.checkResourceExist(projectId);
        return super.list(projectId, scene);
    }

    @Override
    public CustomFieldDTO getCustomFieldDTOWithCheck(String id) {
        CustomFieldDTO customField = super.getCustomFieldDTOWithCheck(id);
        projectService.checkResourceExist(customField.getScopeId());
        return customField;
    }

    @Override
    public CustomField add(CustomField customField, List<CustomFieldOptionRequest> options) {
        Project project = projectService.checkResourceExist(customField.getScopeId());
        checkProjectTemplateEnable(project.getOrganizationId(), customField.getScene());
        customField.setScopeType(TemplateScopeType.PROJECT.name());
        return super.add(customField, options);
    }

    @Override
    public CustomField update(CustomField customField, List<CustomFieldOptionRequest> options) {
        CustomField originCustomField = getWithCheck(customField.getId());
        if (originCustomField.getInternal()) {
            // 内置字段不能修改名字
            customField.setName(null);
        }
        customField.setScopeId(originCustomField.getScopeId());
        customField.setScene(originCustomField.getScene());
        Project project = projectService.checkResourceExist(originCustomField.getScopeId());
        checkProjectTemplateEnable(project.getOrganizationId(), originCustomField.getScene());
        return super.update(customField, options);
    }

    @Override
    public void delete(String id) {
        CustomField customField = getWithCheck(id);
        checkInternal(customField);
        Project project = projectService.checkResourceExist(customField.getScopeId());
        checkProjectTemplateEnable(project.getOrganizationId(), customField.getScene());
        super.delete(id);
    }

    private void checkProjectTemplateEnable(String orgId, String scene) {
        if (isOrganizationTemplateEnable(orgId, scene)) {
            throw new MSException(PROJECT_TEMPLATE_PERMISSION);
        }
    }
}