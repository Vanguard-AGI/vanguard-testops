package io.vanguard.testops.functional.support.importexport.excel.constants;


import io.vanguard.testops.functional.support.importexport.excel.domain.FunctionalCaseExcelData;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Jan
 */

public enum FunctionalCaseImportField {

    ID("id", "ID", "ID", "ID", FunctionalCaseExcelData::getNum),
    NAME("name", "用例名称", "用例名稱", "Name", FunctionalCaseExcelData::getName),
    MODULE("module", "所属模块", "所屬模塊", "Module", FunctionalCaseExcelData::getModule),
    TAGS("tags", "标签", "標簽", "Tag", FunctionalCaseImportField::parseTags),
    PREREQUISITE("prerequisite", "前置条件", "前置條件", "Prerequisite", FunctionalCaseExcelData::getPrerequisite),
    TEXT_DESCRIPTION("textDescription", "步骤描述", "步驟描述", "Text description", FunctionalCaseExcelData::getTextDescription),
    EXPECTED_RESULT("expectedResult", "预期结果", "預期結果", "Expected result", FunctionalCaseExcelData::getExpectedResult),
    CASE_EDIT_TYPE("caseEditType", "编辑模式", "編輯模式", "Case edit type", FunctionalCaseExcelData::getCaseEditType),
    DESCRIPTION("description", "备注", "備註", "Description", FunctionalCaseExcelData::getDescription),
    /** 来源用例ID，用于导出再导入复用统计；导出时两库行填 id，导入时有值则写 functional_case.source_case_id */
    SOURCE_CASE_ID("source_case_id", "source_case_id", "source_case_id", "source_case_id",
            data -> data.getSourceCaseId() != null ? data.getSourceCaseId() : "");

    private Map<Locale, String> fieldLangMap;
    private Function<FunctionalCaseExcelData, String> parseFunc;
    private String value;

    FunctionalCaseImportField(String value, String zn, String chineseTw, String us, Function<FunctionalCaseExcelData, String> parseFunc) {
        this.fieldLangMap = new HashMap<Locale, String>();
        fieldLangMap.put(Locale.SIMPLIFIED_CHINESE, zn);
        fieldLangMap.put(Locale.TRADITIONAL_CHINESE, chineseTw);
        fieldLangMap.put(Locale.US, us);
        this.value = value;
        this.parseFunc = parseFunc;
    }

    public Map<Locale, String> getFieldLangMap() {
        return this.fieldLangMap;
    }

    public String getValue() {
        return value;
    }

    public String parseExcelDataValue(FunctionalCaseExcelData excelData) {
        return parseFunc.apply(excelData);
    }

    private static String parseTags(FunctionalCaseExcelData excelData) {
        String tags = StringUtils.EMPTY;
        try {
            if (excelData.getTags() != null) {
                List arr = JSON.parseArray(excelData.getTags());
                if (CollectionUtils.isNotEmpty(arr)) {
                    tags = StringUtils.joinWith(",", arr.toArray());
                }
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return tags;
    }

    public boolean containsHead(String head) {
        return fieldLangMap.values().contains(head);
    }
}
