package io.vanguard.testops.functional.support.importexport.excel.converter;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.vanguard.testops.sdk.util.DateUtils;
import io.vanguard.testops.sdk.util.Translator;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan
 */
public class FunctionalCaseExportReviewCommentConverter implements FunctionalCaseExportConverter {

    private Map<String, String> reviewStatusMap = new HashMap<>();

    public FunctionalCaseExportReviewCommentConverter() {
        for (FunctionalCaseReviewStatus value : FunctionalCaseReviewStatus.values()) {
            reviewStatusMap.put(value.name(), value.getI18nKey());
        }
    }

    @Override
    public String parse(FunctionalCase functionalCase, Map<String, List<FunctionalCaseComment>> caseCommentMap, Map<String, List<TestPlanCaseExecuteHistory>> executeCommentMap, Map<String, List<CaseReviewHistory>> reviewCommentMap) {
        if (reviewCommentMap.containsKey(functionalCase.getId())) {
            StringBuilder result = new StringBuilder();
            String template = Translator.get("functional_case_review_comment_template");
            List<CaseReviewHistory> reviewComent = reviewCommentMap.get(functionalCase.getId());
            reviewComent.forEach(item -> {
                String status = getFromMapOfNullableWithTranslate(reviewStatusMap, item.getStatus());
                String createTime = DateUtils.getTimeString(item.getCreateTime());
                String content = parseHtml(new String(item.getContent() == null ? new byte[0] : item.getContent(), StandardCharsets.UTF_8));
                result.append(String.format(template, item.getCreateUser(), status, createTime, content));
            });
            return result.toString();
        }
        return StringUtils.EMPTY;
    }
}
