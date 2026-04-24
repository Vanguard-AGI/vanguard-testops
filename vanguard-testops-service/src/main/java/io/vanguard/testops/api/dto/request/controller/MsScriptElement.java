package io.vanguard.testops.api.dto.request.controller;

import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.project.constants.ScriptLanguageType;
import io.vanguard.testops.project.dto.CommonScriptInfo;
import io.vanguard.testops.sdk.valid.EnumValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 公共脚本组件
 * 主要用于公共脚本测试执行时，生成jmx
 */
@Data
public class MsScriptElement extends AbstractMsTestElement {
    /**
     * 脚本内容
     */
    private String script;
    /**
     * 脚本语言
     * {@link ScriptLanguageType}
     */
    @Size(max = 20)
    @NotBlank
    @EnumValue(enumClass = ScriptLanguageType.class)
    private String scriptLanguage;
    /**
     * 是否启用公共脚本
     * 默认为 false
     * 环境脚本无须配置
     */
    private Boolean enableCommonScript = false;
    /**
     * 公共脚本信息
     * {@link CommonScriptInfo}
     */
    @Valid
    private CommonScriptInfo commonScriptInfo;
}