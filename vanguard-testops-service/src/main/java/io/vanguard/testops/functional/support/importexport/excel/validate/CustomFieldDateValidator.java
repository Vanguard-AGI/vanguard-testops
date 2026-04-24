package io.vanguard.testops.functional.support.importexport.excel.validate;


import io.vanguard.testops.functional.support.importexport.excel.exception.CustomFieldValidateException;
import io.vanguard.testops.sdk.util.DateUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jan
 */
public class CustomFieldDateValidator extends AbstractCustomFieldValidator {

    public void validate(TemplateCustomFieldDTO customField, String value) throws CustomFieldValidateException {
        validateRequired(customField, value);
        try {
            if (StringUtils.isNotBlank(value)) {
                DateUtils.getDate(value);
            }
        } catch (Exception e) {
            CustomFieldValidateException.throwException(String.format(Translator.get("custom_field_date_tip"), customField.getFieldName(), DateUtils.DATE_PATTERN));
        }
    }
}
