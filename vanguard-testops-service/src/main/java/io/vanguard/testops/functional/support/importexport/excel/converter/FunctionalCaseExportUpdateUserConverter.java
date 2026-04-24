package io.vanguard.testops.functional.support.importexport.excel.converter;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;

import java.util.List;
import java.util.Map;

/**
 * @author Jan
 */
public class FunctionalCaseExportUpdateUserConverter extends FunctionalCaseExportCreateUserConverter {


    public FunctionalCaseExportUpdateUserConverter(String projectId) {
        super(projectId);
    }

    @Override
    public String parse(FunctionalCase functionalCase, Map<String, List<FunctionalCaseComment>> caseCommentMap, Map<String, List<TestPlanCaseExecuteHistory>> executeCommentMap, Map<String, List<CaseReviewHistory>> reviewCommentMap) {
        return getFromMapOfNullable(userMap, functionalCase.getUpdateUser());
    }
}
