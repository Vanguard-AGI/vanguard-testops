package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.dto.PluginDTO;
import io.vanguard.testops.system.dto.sdk.OptionDTO;

import java.util.List;

public interface ExtPluginMapper {
    List<PluginDTO> getPlugins();

    List<OptionDTO> selectPluginOptions(List<String> pluginIds);
}
