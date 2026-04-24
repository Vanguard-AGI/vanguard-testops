package io.vanguard.testops.system.support.plugin;

import lombok.Data;

@Data
public class PluginNotifiedDTO {
    private String operate;
    private String pluginId;
    private String fileName;
}
