package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.domain.CustomField;
import io.vanguard.testops.system.domain.TemplateCustomField;
import io.vanguard.testops.system.domain.TemplateCustomFieldExample;
import io.vanguard.testops.system.dto.CustomFieldDTO;
import io.vanguard.testops.system.dto.sdk.request.TemplateCustomFieldRequest;
import io.vanguard.testops.system.mapper.TemplateCustomFieldMapper;
import io.vanguard.testops.system.resolver.field.AbstractCustomFieldResolver;
import io.vanguard.testops.system.resolver.field.CustomFieldResolverFactory;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseTemplateCustomFieldService {
    @Resource
    private TemplateCustomFieldMapper templateCustomFieldMapper;
    @Resource
    private BaseCustomFieldService baseCustomFieldService;

    /**
     * 是否校验默认值
     */
    public static final ThreadLocal<Boolean> validateDefaultValue = new ThreadLocal<>();

    public void deleteByTemplateId(String templateId) {
        TemplateCustomFieldExample example = new TemplateCustomFieldExample();
        example.createCriteria()
                .andTemplateIdEqualTo(templateId);
        templateCustomFieldMapper.deleteByExample(example);
    }

    public void deleteByTemplateIdAndSystem(String templateId, boolean isSystem) {
        TemplateCustomFieldExample example = new TemplateCustomFieldExample();
        example.createCriteria()
                .andTemplateIdEqualTo(templateId)
                .andSystemFieldEqualTo(isSystem);
        templateCustomFieldMapper.deleteByExample(example);
    }

    public void deleteByTemplateIds(List<String> projectTemplateIds) {
        if (CollectionUtils.isEmpty(projectTemplateIds)) {
            return;
        }
        TemplateCustomFieldExample example = new TemplateCustomFieldExample();
        example.createCriteria().andTemplateIdIn(projectTemplateIds);
        templateCustomFieldMapper.deleteByExample(example);
    }

    public void addCustomFieldByTemplateId(String id, List<TemplateCustomFieldRequest> customFieldRequests) {
        if (CollectionUtils.isEmpty(customFieldRequests)) {
            return;
        }
        // 过滤下不存在的字段
        List<String> ids = customFieldRequests.stream().map(TemplateCustomFieldRequest::getFieldId).toList();
        Set<String> fieldIdSet = baseCustomFieldService.getByIds(ids)
                .stream()
                .map(CustomField::getId)
                .collect(Collectors.toSet());
        customFieldRequests = customFieldRequests.stream()
                .filter(item -> fieldIdSet.contains(item.getFieldId()))
                .toList();
        this.addByTemplateId(id, customFieldRequests, false);
    }

    public void addSystemFieldByTemplateId(String id, List<TemplateCustomFieldRequest> customFieldRequests) {
        if (CollectionUtils.isEmpty(customFieldRequests)) {
            return;
        }
        this.addByTemplateId(id, customFieldRequests, true);
    }

    private void addByTemplateId(String templateId, List<TemplateCustomFieldRequest> customFieldRequests, boolean isSystem) {
        AtomicReference<Integer> pos = new AtomicReference<>(0);
        List<TemplateCustomField> templateCustomFields = customFieldRequests.stream().map(field -> {
            TemplateCustomField templateCustomField = new TemplateCustomField();
            templateCustomField.setId(IDGenerator.nextStr());
            BeanUtils.copyBean(templateCustomField, field);
            templateCustomField.setTemplateId(templateId);
            templateCustomField.setPos(pos.getAndSet(pos.get() + 1));
            templateCustomField.setDefaultValue(isSystem ? field.getDefaultValue().toString() : parseDefaultValue(field));
            templateCustomField.setSystemField(isSystem);
            return templateCustomField;
        }).toList();
        if (templateCustomFields.size() > 0) {
            templateCustomFieldMapper.batchInsert(templateCustomFields);
        }
    }

    private String parseDefaultValue(TemplateCustomFieldRequest field) {
        CustomField customField = baseCustomFieldService.getWithCheck(field.getFieldId());
        AbstractCustomFieldResolver customFieldResolver = CustomFieldResolverFactory.getResolver(customField.getType());
        CustomFieldDTO customFieldDTO = BeanUtils.copyBean(new CustomFieldDTO(), customField);
        customFieldDTO.setRequired(false);
        if (BooleanUtils.isNotFalse(validateDefaultValue.get())) {
            // 创建项目时不校验默认值
            customFieldResolver.validate(customFieldDTO, field.getDefaultValue());
        }
        return customFieldResolver.parse2String(field.getDefaultValue());
    }

    public List<TemplateCustomField> getByTemplateId(String id) {
        TemplateCustomFieldExample example = new TemplateCustomFieldExample();
        example.createCriteria().andTemplateIdEqualTo(id);
        return templateCustomFieldMapper.selectByExample(example);
    }

    public List<TemplateCustomField> getByTemplateIds(List<String> projectTemplateIds) {
        if (CollectionUtils.isEmpty(projectTemplateIds)) {
            return new ArrayList(0);
        }
        TemplateCustomFieldExample example = new TemplateCustomFieldExample();
        example.createCriteria().andTemplateIdIn(projectTemplateIds);
        return templateCustomFieldMapper.selectByExample(example);
    }
}
