package io.vanguard.testops.functional.support.importexport.excel.converter;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan
 */
public class FunctionalCaseExportExecuteStatusConverter implements FunctionalCaseExportConverter {

    private Map<String, String> executeStatusMap = new HashMap<>();

    public FunctionalCaseExportExecuteStatusConverter() {
        for (FunctionalCaseExecuteStatus value : FunctionalCaseExecuteStatus.values()) {
            executeStatusMap.put(value.name(), value.getI18nKey());
        }
    }


    @Override
    public String parse(FunctionalCase functionalCase, Map<String, List<FunctionalCaseComment>> caseCommentMap, Map<String, List<TestPlanCaseExecuteHistory>> executeCommentMap, Map<String, List<CaseReviewHistory>> reviewCommentMap) {
        return getFromMapOfNullableWithTranslate(executeStatusMap, functionalCase.getLastExecuteResult());
    }
}
