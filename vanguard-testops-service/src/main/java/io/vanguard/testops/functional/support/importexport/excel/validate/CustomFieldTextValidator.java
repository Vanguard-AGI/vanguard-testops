package io.vanguard.testops.functional.support.importexport.excel.validate;

import io.vanguard.testops.functional.support.importexport.excel.exception.CustomFieldValidateException;
import io.vanguard.testops.sdk.constants.CustomFieldType;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jan
 */
public class CustomFieldTextValidator extends AbstractCustomFieldValidator {
    protected static final int INPUT_LENGTH = 255;
    protected static final int TEXTAREA_LENGTH = 1000;

    public void validate(TemplateCustomFieldDTO customField, String value) throws CustomFieldValidateException {
        validateRequired(customField, value);
        if (StringUtils.equalsIgnoreCase(CustomFieldType.INPUT.name(), customField.getType()) && value.length() > INPUT_LENGTH) {
            //输入框 255
            CustomFieldValidateException.throwException(String.format(Translator.get("custom_field_input_length_tip"), customField.getFieldName()));
        }
        if (StringUtils.equalsIgnoreCase(CustomFieldType.TEXTAREA.name(), customField.getType()) && value.length() > TEXTAREA_LENGTH) {
            //文本域 1000
            CustomFieldValidateException.throwException(String.format(Translator.get("custom_field_textarea_length_tip"), customField.getFieldName()));
        }
    }
}
