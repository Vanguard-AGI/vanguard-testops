package io.vanguard.testops.api.dto.request.controller.loop;

import io.vanguard.testops.api.dto.request.controller.ConditionUtils;
import io.vanguard.testops.sdk.valid.EnumValue;
import lombok.Data;

@Data
public class MsWhileVariable {
    /**
     * 变量 255
     */
    private String variable;
    /**
     * 操作符
     */
    @EnumValue(enumClass = io.vanguard.testops.sdk.constants.MsAssertionCondition.class)
    private String condition;
    /**
     * 值 255
     */
    private String value;

    public String getConditionValue() {
        ConditionUtils conditionUtils = new ConditionUtils();
        return conditionUtils.getConditionValue(variable, condition, value);
    }
}

