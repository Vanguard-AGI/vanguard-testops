package io.vanguard.testops.system.resolver.field;


import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.dto.CustomFieldDTO;

public class CustomFieldMultipleMemberResolver extends CustomFieldMemberResolver {

    @Override
    public void validate(CustomFieldDTO customField, Object value) {
        validateArrayRequired(customField, value);
        validateArray(customField.getName(), value);
    }

    @Override
    public String parse2String(Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(String value) {
        return parse2Array(value);
    }
}
