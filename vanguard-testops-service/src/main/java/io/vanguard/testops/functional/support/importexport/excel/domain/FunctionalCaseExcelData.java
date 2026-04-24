package io.vanguard.testops.functional.support.importexport.excel.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.metadata.data.WriteCellData;
import io.vanguard.testops.functional.support.importexport.excel.constants.FunctionalCaseImportField;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Jan
 */
@Getter
@Setter
public class FunctionalCaseExcelData {
    @ExcelIgnore
    private String num;
    @ExcelIgnore
    private String name;
    @ExcelIgnore
    private String module;
    @ExcelIgnore
    private String tags;
    @ExcelIgnore
    private String prerequisite;
    @ExcelIgnore
    private String description;
    @ExcelIgnore
    private String textDescription;
    @ExcelIgnore
    private String expectedResult;
    @ExcelIgnore
    private String caseEditType;
    @ExcelIgnore
    private String sourceCaseId;
    @ExcelIgnore
    private String steps;
    @ExcelIgnore
    Map<String, Object> customData = new LinkedHashMap<>();
    @ExcelIgnore
    Map<String, String> otherFields;

    @ExcelIgnore
    private WriteCellData<String> hyperLinkName;

    /**
     * 合并文本描述
     */
    @ExcelIgnore
    List<String> MergeTextDescription;
    /**
     * 合并步骤结果
     */
    @ExcelIgnore
    List<String> mergeExpectedResult;


    public List<List<String>> getHead(List<TemplateCustomFieldDTO> customFields) {
        return new ArrayList<>();
    }

    public List<List<String>> getHead(List<TemplateCustomFieldDTO> customFields, Locale lang) {
        List<List<String>> heads = new ArrayList<>();
        FunctionalCaseImportField[] fields = FunctionalCaseImportField.values();
        for (FunctionalCaseImportField field : fields) {
            if (!StringUtils.equalsIgnoreCase(field.name(), "ID")) {
                heads.add(Arrays.asList(field.getFieldLangMap().get(lang)));
            }
        }

        if (CollectionUtils.isNotEmpty(customFields)) {
            for (TemplateCustomFieldDTO dto : customFields) {
                List<String> list = new ArrayList<>();
                list.add(dto.getFieldName());
                heads.add(list);
            }
        }
        return heads;
    }
}
