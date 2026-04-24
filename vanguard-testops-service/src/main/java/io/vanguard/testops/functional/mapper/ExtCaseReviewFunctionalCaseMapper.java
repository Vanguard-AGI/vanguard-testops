package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseReviewFunctionalCase;
import io.vanguard.testops.functional.dto.FunctionalCaseModuleCountDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseReviewDTO;
import io.vanguard.testops.functional.dto.ReviewFunctionalCaseDTO;
import io.vanguard.testops.functional.request.BaseReviewCaseBatchRequest;
import io.vanguard.testops.functional.request.FunctionalCaseReviewListRequest;
import io.vanguard.testops.functional.request.ReviewFunctionalCasePageRequest;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtCaseReviewFunctionalCaseMapper {
    @BaseConditionFilter
    List<FunctionalCaseReviewDTO> list(@Param("request") FunctionalCaseReviewListRequest request);

    void updateStatus(@Param("caseId") String caseId, @Param("reviewId") String reviewId, @Param("status") String status);

    List<String> getCaseIdsByReviewId(@Param("reviewId") String reviewId);

    @BaseConditionFilter
    List<ReviewFunctionalCaseDTO> page(@Param("request") ReviewFunctionalCasePageRequest request, @Param("deleted") boolean deleted, @Param("sort") String sort);

    Long getPos(@Param("reviewId") String reviewId);

    Long getPrePos(@Param("reviewId") String reviewId, @Param("basePos") Long basePos);

    Long getLastPos(@Param("reviewId") String reviewId, @Param("basePos") Long basePos);

    @BaseConditionFilter
    List<String> getIds(@Param("request") BaseReviewCaseBatchRequest request, @Param("deleted") boolean deleted);

    @BaseConditionFilter
    List<CaseReviewFunctionalCase> getListByRequest(@Param("request") BaseReviewCaseBatchRequest request, @Param("deleted") boolean deleted);

    List<CaseReviewFunctionalCase> getList(@Param("reviewId") String reviewId, @Param("reviewIds") List<String> reviewIds, @Param("deleted") boolean deleted);


    List<CaseReviewFunctionalCase> getListExcludes(@Param("reviewIds") List<String> reviewIds, @Param("caseIds") List<String> caseIds, @Param("deleted") boolean deleted);

    List<CaseReviewFunctionalCase> getCaseIdsByIds(@Param("ids") List<String> ids);

    @BaseConditionFilter
    List<FunctionalCaseModuleCountDTO> countModuleIdByRequest(@Param("request") ReviewFunctionalCasePageRequest request, @Param("deleted") boolean deleted);

    @BaseConditionFilter
    long caseCount(@Param("request") ReviewFunctionalCasePageRequest request, @Param("deleted") boolean deleted);

}
