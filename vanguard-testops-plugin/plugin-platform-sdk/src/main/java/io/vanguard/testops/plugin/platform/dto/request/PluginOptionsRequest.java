package io.vanguard.testops.plugin.platform.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Jan
 */
@Setter
@Getter
public class PluginOptionsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String projectConfig;
    private String optionMethod;

}
