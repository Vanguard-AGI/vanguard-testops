package io.vanguard.testops.functional.support.importexport.excel.handler;

import com.alibaba.excel.util.BooleanUtils;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import io.vanguard.testops.functional.support.importexport.excel.constants.FunctionalCaseImportField;
import io.vanguard.testops.sdk.constants.CustomFieldType;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author Jan
 */
public class FunctionCaseTemplateWriteHandler implements RowWriteHandler {

    Map<String, List<String>> customFieldOptionsMap;

    private Sheet sheet;
    private Drawing<?> drawingPatriarch;

    private Map<String, TemplateCustomFieldDTO> customField;
    private Map<String, Integer> fieldMap = new HashMap<>();

    public FunctionCaseTemplateWriteHandler(List<List<String>> headList, Map<String, List<String>> customFieldOptionsMap, Map<String, TemplateCustomFieldDTO> customFieldMap) {
        initIndex(headList);
        this.customFieldOptionsMap = customFieldOptionsMap;
        this.customField = customFieldMap;
    }

    private void initIndex(List<List<String>> headList) {
        int index = 0;
        for (List<String> list : headList) {
            for (String head : list) {
                this.fieldMap.put(head, index);
                index++;
            }
        }
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        if (BooleanUtils.isTrue(context.getHead())) {
            sheet = context.getWriteSheetHolder().getSheet();
            drawingPatriarch = sheet.createDrawingPatriarch();

            Iterator<Map.Entry<String, Integer>> iterator = fieldMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                //默认字段
                if (FunctionalCaseImportField.ID.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.id"));
                }
                if (FunctionalCaseImportField.NAME.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
                if (FunctionalCaseImportField.MODULE.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required").concat("，").concat(Translator.get("module_created_automatically")));
                }
                if (FunctionalCaseImportField.TAGS.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.tag"));
                }
                if (FunctionalCaseImportField.CASE_EDIT_TYPE.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.case_edit_type"));
                }
                if (FunctionalCaseImportField.TEXT_DESCRIPTION.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.text_description"));
                }
                if (FunctionalCaseImportField.PREREQUISITE.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
                if (FunctionalCaseImportField.EXPECTED_RESULT.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.not_required"));
                }
                if (FunctionalCaseImportField.DESCRIPTION.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.not_required"));
                }
                // P9: source_case_id 列隐藏且列宽为 0，不在默认视图中显示
                if (FunctionalCaseImportField.SOURCE_CASE_ID.containsHead(entry.getKey())) {
                    Integer colIndex = fieldMap.get(entry.getKey());
                    if (colIndex != null) {
                        sheet.setColumnHidden(colIndex, true);
                        sheet.setColumnWidth(colIndex, 0);
                    }
                }

                //自定义字段
                if (customField.containsKey(entry.getKey())) {
                    TemplateCustomFieldDTO templateCustomFieldDTO = customField.get(entry.getKey());
                    List<String> strings = customFieldOptionsMap.get(entry.getKey());
                    if (StringUtils.equalsAnyIgnoreCase(templateCustomFieldDTO.getType(), CustomFieldType.MULTIPLE_MEMBER.name(), CustomFieldType.MEMBER.name())) {
                        if (templateCustomFieldDTO.getRequired()) {
                            setComment(fieldMap.get(entry.getKey()), Translator.get("required").concat(",").concat(Translator.get("excel.template.member")));
                        } else {
                            setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.not_required").concat(",").concat(Translator.get("excel.template.member")));
                        }
                    } else {
                        if (templateCustomFieldDTO.getRequired()) {
                            if (CollectionUtils.isNotEmpty(strings)) {
                                setComment(fieldMap.get(entry.getKey()), Translator.get("required").concat("：").concat(Translator.get("options")).concat(JSON.toJSONString(customFieldOptionsMap.get(entry.getKey()))));
                            } else {
                                setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                            }
                        } else {
                            if (CollectionUtils.isNotEmpty(strings)) {
                                setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.not_required").concat("：").concat(Translator.get("options")).concat(JSON.toJSONString(customFieldOptionsMap.get(entry.getKey()))));
                            } else {
                                setComment(fieldMap.get(entry.getKey()), Translator.get("excel.template.not_required"));
                            }
                        }
                    }
                }
            }
        }

    }

    private void setComment(Integer index, String text) {
        if (index == null) {
            return;
        }
        Comment comment = drawingPatriarch.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, index, 0, index + 3, 1));
        comment.setString(new XSSFRichTextString(text));
        sheet.getRow(0).getCell(index).setCellComment(comment);
    }

    public static HorizontalCellStyleStrategy getHorizontalWrapStrategy() {
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // 设置自动换行
        contentWriteCellStyle.setWrapped(true);
        return new HorizontalCellStyleStrategy(null, contentWriteCellStyle);
    }
}
