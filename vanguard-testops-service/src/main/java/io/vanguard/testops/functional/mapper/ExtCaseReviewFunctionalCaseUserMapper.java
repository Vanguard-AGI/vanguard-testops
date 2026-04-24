package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.dto.ReviewsDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtCaseReviewFunctionalCaseUserMapper {
    List<ReviewsDTO> selectReviewers(@Param("ids") List<String> ids, @Param("reviewId") String reviewId);

    void deleteByCaseIds(@Param("ids") List<String> ids, @Param("reviewId") String reviewId);
}