package io.vanguard.testops.system.service;

import io.vanguard.testops.plugin.platform.dto.request.PlatformRequest;
import io.vanguard.testops.plugin.platform.spi.Platform;
import io.vanguard.testops.sdk.constants.PluginScenarioType;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.domain.Plugin;
import io.vanguard.testops.system.domain.ServiceIntegration;
import io.vanguard.testops.system.domain.ServiceIntegrationExample;
import io.vanguard.testops.system.mapper.ServiceIntegrationMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class PlatformPluginService {

    @Resource
    private PluginLoadService pluginLoadService;
    @Resource
    private ServiceIntegrationMapper serviceIntegrationMapper;
    @Resource
    private BasePluginService basePluginService;

    /**
     * 获取平台实例
     *
     * @param pluginId
     * @param orgId
     * @param integrationConfig
     * @return
     */
    public Platform getPlatform(String pluginId, String orgId, String integrationConfig) {
        basePluginService.checkPluginEnableAndPermission(pluginId, orgId);
        PlatformRequest pluginRequest = new PlatformRequest();
        pluginRequest.setIntegrationConfig(integrationConfig);
        pluginRequest.setOrganizationId(orgId);
        return pluginLoadService.getImplInstance(Platform.class, pluginId, pluginRequest);
    }

    public Platform getPlatform(String pluginId, String orgId) {
        ServiceIntegration serviceIntegration = getServiceIntegrationByPluginId(pluginId, orgId);
        if (serviceIntegration == null) {
            throw new MSException(Translator.get("service_integration.configuration.not_blank"));
        }
        return getPlatform(pluginId, orgId, new String(serviceIntegration.getConfiguration()));
    }

    private ServiceIntegration getServiceIntegrationByPluginId(String pluginId, String orgId) {
        ServiceIntegrationExample example = new ServiceIntegrationExample();
        example.createCriteria().andPluginIdEqualTo(pluginId).andOrganizationIdEqualTo(orgId);
        List<ServiceIntegration> serviceIntegrations = serviceIntegrationMapper.selectByExampleWithBLOBs(example);
        return CollectionUtils.isEmpty(serviceIntegrations) ? null :  serviceIntegrations.getFirst();
    }

    public List<Plugin> getOrgEnabledPlatformPlugins(String orgId) {
        return basePluginService.getOrgEnabledPlugins(orgId, PluginScenarioType.PLATFORM);
    }
}