package io.vanguard.testops.functional.support.importexport.excel.converter;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.vanguard.testops.sdk.util.DateUtils;
import io.vanguard.testops.sdk.util.Translator;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Jan
 */
public class FunctionalCaseExportCaseCommentConverter implements FunctionalCaseExportConverter {

    @Override
    public String parse(FunctionalCase functionalCase, Map<String, List<FunctionalCaseComment>> caseCommentMap, Map<String, List<TestPlanCaseExecuteHistory>> executeCommentMap, Map<String, List<CaseReviewHistory>> reviewCommentMap) {
        if (caseCommentMap.containsKey(functionalCase.getId())) {
            StringBuilder result = new StringBuilder();
            String template = Translator.get("functional_case_comment_template");
            List<FunctionalCaseComment> caseComments = caseCommentMap.get(functionalCase.getId());
            caseComments.forEach(item -> {
                String updateTime = DateUtils.getTimeString(item.getUpdateTime());
                String content = parseHtml(item.getContent());
                result.append(String.format(template, item.getCreateUser(), updateTime, content));
            });
            return result.toString();
        }
        return StringUtils.EMPTY;
    }
}
