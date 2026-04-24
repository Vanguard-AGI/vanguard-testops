package io.vanguard.testops.system.resolver.field;

import io.vanguard.testops.system.dto.CustomFieldDTO;

public class CustomFieldTextResolver extends AbstractCustomFieldResolver {

    @Override
    public void validate(CustomFieldDTO customField, Object value) {
        validateRequired(customField, value);
        validateString(customField.getName(), value);
    }

}
