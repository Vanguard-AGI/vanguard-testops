package io.vanguard.testops.project.api.assertion;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.vanguard.testops.sdk.valid.EnumValue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: Jan
 * @CreateTime: 2023-11-22  15:33
 */
@Data
@JsonTypeName("RESPONSE_CODE")
public class MsResponseCodeAssertion extends MsAssertion {
    /**
     * 匹配条件
     * 不校验即忽略状态
     * 选择其他条件时，也忽略状态
     * 不校验可搭配其他校验使用
     * 取值参考 {@link io.vanguard.testops.sdk.constants.MsAssertionCondition}
     */
    @NotBlank
    @EnumValue(enumClass = io.vanguard.testops.sdk.constants.MsAssertionCondition.class)
    private String condition;
    /**
     * 匹配值
     */
    private String expectedValue;
}
