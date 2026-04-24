package io.vanguard.testops.system.dto;

import lombok.Data;

@Data
public class DisplayConfigDTO {
    private String themeColor;
    private String systemName;
    private String systemDescription;
    private Boolean enableCustomTheme;
    private Boolean enableCustomLogo;
}
