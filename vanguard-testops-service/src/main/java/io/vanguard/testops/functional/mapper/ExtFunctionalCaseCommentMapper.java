package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseReviewHistory;
import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtFunctionalCaseCommentMapper {
    List<FunctionalCaseComment> getCaseComment(@Param("ids") List<String> ids);

    List<TestPlanCaseExecuteHistory> getExecuteComment(@Param("ids") List<String> ids);

    List<CaseReviewHistory> getReviewComment(@Param("ids") List<String> ids);
}
