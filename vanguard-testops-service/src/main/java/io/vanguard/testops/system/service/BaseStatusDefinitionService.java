package io.vanguard.testops.system.service;

import io.vanguard.testops.system.domain.StatusDefinition;
import io.vanguard.testops.system.domain.StatusDefinitionExample;
import io.vanguard.testops.system.mapper.StatusDefinitionMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseStatusDefinitionService {

    @Resource
    private StatusDefinitionMapper statusDefinitionMapper;

    public List<StatusDefinition> getStatusDefinitions(List<String> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) {
            return List.of();
        }
        StatusDefinitionExample example = new StatusDefinitionExample();
        example.createCriteria()
                .andStatusIdIn(statusIds);
        return statusDefinitionMapper.selectByExample(example);
    }

    public void deleteByStatusId(String id) {
        StatusDefinitionExample example = new StatusDefinitionExample();
        example.createCriteria()
                .andStatusIdEqualTo(id);
        statusDefinitionMapper.deleteByExample(example);
    }

    public void delete(String statusId, String definitionId) {
        statusDefinitionMapper.deleteByPrimaryKey(statusId, definitionId);
    }

    public void add(StatusDefinition statusDefinition) {
        StatusDefinitionExample example = new StatusDefinitionExample();
        example.createCriteria()
                .andStatusIdEqualTo(statusDefinition.getStatusId())
                .andDefinitionIdEqualTo(statusDefinition.getDefinitionId());
        statusDefinitionMapper.insert(statusDefinition);
    }

    public void deleteByStatusIds(List<String> statusItemIds) {
        if (CollectionUtils.isEmpty(statusItemIds)) {
            return;
        }
        StatusDefinitionExample example = new StatusDefinitionExample();
        example.createCriteria()
                .andStatusIdIn(statusItemIds);
        statusDefinitionMapper.deleteByExample(example);
    }

    public void batchAdd(List<StatusDefinition> statusDefinitions) {
        if (CollectionUtils.isEmpty(statusDefinitions)) {
            return;
        }
        statusDefinitionMapper.batchInsert(statusDefinitions);
    }

    public void deleteByStatusIdsAndDefinitionId(List<String> statusIds, String definitionId) {
        StatusDefinitionExample example = new StatusDefinitionExample();
        example.createCriteria()
                .andStatusIdIn(statusIds)
                .andDefinitionIdEqualTo(definitionId);
        statusDefinitionMapper.deleteByExample(example);
    }
}