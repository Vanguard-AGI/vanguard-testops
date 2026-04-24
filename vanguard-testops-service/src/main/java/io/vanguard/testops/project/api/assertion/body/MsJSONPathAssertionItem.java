package io.vanguard.testops.project.api.assertion.body;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vanguard.testops.sdk.constants.MsAssertionCondition;
import io.vanguard.testops.sdk.valid.EnumValue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * JSONPath断言
 *
 * @Author: Jan
 * @CreateTime: 2023-11-23  14:04
 */
@Data
public class MsJSONPathAssertionItem extends MsBodyAssertionItem {
    /**
     * 表达式
     */
    private String expression;
    /**
     * 匹配条件
     * 取值参考 {@link io.vanguard.testops.sdk.constants.MsAssertionCondition}
     */
    @NotBlank
    @EnumValue(enumClass = io.vanguard.testops.sdk.constants.MsAssertionCondition.class)
    private String condition;
    /**
     * 匹配值
     */
    private String expectedValue;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotBlank(expression)
                && StringUtils.isNotBlank(condition)
                && BooleanUtils.isTrue(this.getEnable()
                && !StringUtils.equals(condition, MsAssertionCondition.UNCHECK.name()));
    }
}

