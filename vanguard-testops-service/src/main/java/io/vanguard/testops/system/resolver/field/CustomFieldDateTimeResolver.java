package io.vanguard.testops.system.resolver.field;


import io.vanguard.testops.sdk.util.DateUtils;
import io.vanguard.testops.system.dto.CustomFieldDTO;
import org.apache.commons.lang3.StringUtils;

public class CustomFieldDateTimeResolver extends AbstractCustomFieldResolver {

    @Override
    public void validate(CustomFieldDTO customField, Object value) {
        validateRequired(customField, value);
        try {
            if (value != null && StringUtils.isNotBlank(value.toString())) {
                DateUtils.getTime(value.toString());
            }
        } catch (Exception e) {
            throwValidateException(customField.getName());
        }
    }

}
