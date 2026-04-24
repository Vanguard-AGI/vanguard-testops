package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.dto.CaseReviewUserDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtCaseReviewUserMapper {

    List<CaseReviewUserDTO> getReviewUser(@Param("reviewIds") List<String> reviewIds);



}
