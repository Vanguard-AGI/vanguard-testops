package io.vanguard.testops.system.service;

import io.vanguard.testops.system.domain.PluginOrganization;
import io.vanguard.testops.system.domain.PluginOrganizationExample;
import io.vanguard.testops.system.mapper.PluginOrganizationMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class BasePluginOrganizationService {
    @Resource
    private PluginOrganizationMapper pluginOrganizationMapper;


    public List<PluginOrganization> getByPluginIds(List<String> pluginIds) {
        PluginOrganizationExample example = new PluginOrganizationExample();
        example.createCriteria().andPluginIdIn(pluginIds);
        return pluginOrganizationMapper.selectByExample(example);
    }

    public List<PluginOrganization> getByPluginIdAndOrgId(String pluginId, String orgId) {
        if (StringUtils.isBlank(orgId)) {
            return new ArrayList<>(0);
        }
        PluginOrganizationExample example = new PluginOrganizationExample();
        example.createCriteria()
                .andPluginIdEqualTo(pluginId)
                .andOrganizationIdEqualTo(orgId);
        return pluginOrganizationMapper.selectByExample(example);
    }
}
