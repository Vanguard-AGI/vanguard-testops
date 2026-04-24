package io.vanguard.testops.functional.support.importexport.excel.validate;


import io.vanguard.testops.functional.support.importexport.excel.exception.CustomFieldValidateException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jan
 */
public class CustomFieldFloatValidator extends AbstractCustomFieldValidator {

    public void validate(TemplateCustomFieldDTO customField, String value) throws CustomFieldValidateException {
        validateRequired(customField, value);
        try {
            if (StringUtils.isNotBlank(value)) {
                Float.parseFloat(value);
            }
        } catch (Exception e) {
            CustomFieldValidateException.throwException(String.format(Translator.get("custom_field_float_tip"), customField.getFieldName()));
        }
    }
}
