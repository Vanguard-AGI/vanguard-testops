package io.vanguard.testops.functional.support.importexport.excel.converter;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.vanguard.testops.project.service.ProjectApplicationService;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.system.domain.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jan
 */
public class FunctionalCaseExportCreateUserConverter implements FunctionalCaseExportConverter {

    public Map<String, String> userMap = new HashMap<>();

    public FunctionalCaseExportCreateUserConverter(String projectId) {
        ProjectApplicationService projectApplicationService = CommonBeanFactory.getBean(ProjectApplicationService.class);
        List<User> memberOption = projectApplicationService.getProjectUserList(projectId);
        memberOption.forEach(option -> userMap.put(option.getId(), option.getName()));
    }


    @Override
    public String parse(FunctionalCase functionalCase, Map<String, List<FunctionalCaseComment>> caseCommentMap, Map<String, List<TestPlanCaseExecuteHistory>> executeCommentMap, Map<String, List<CaseReviewHistory>> reviewCommentMap) {
        return getFromMapOfNullable(userMap, functionalCase.getCreateUser());
    }
}
