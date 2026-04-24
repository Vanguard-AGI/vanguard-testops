package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.PluginScript;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtPluginScriptMapper {
    List<PluginScript> getOptionByPluginIds(@Param("pluginIds") List<String> pluginIds);
}
