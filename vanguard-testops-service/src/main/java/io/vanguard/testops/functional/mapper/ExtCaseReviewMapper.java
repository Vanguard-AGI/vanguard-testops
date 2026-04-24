package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseReview;
import io.vanguard.testops.functional.dto.CaseReviewDTO;
import io.vanguard.testops.functional.request.CaseReviewBatchRequest;
import io.vanguard.testops.functional.request.CaseReviewPageRequest;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.ProjectCountDTO;
import io.vanguard.testops.project.dto.ProjectUserCreateCount;
import io.vanguard.testops.project.dto.ProjectUserStatusCountDTO;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Jan
 */
public interface ExtCaseReviewMapper {

    List<CaseReview> checkCaseByModuleIds(@Param("moduleIds") List<String> deleteIds);

    Long getPos(@Param("projectId") String projectId);

    @BaseConditionFilter
    List<CaseReviewDTO> list(@Param("request") CaseReviewPageRequest request);

    Long getPrePos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    Long getLastPos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    @BaseConditionFilter
    List<String> getIds(@Param("request") CaseReviewBatchRequest request, @Param("projectId") String projectId);

    void batchMoveModule(@Param("request") CaseReviewBatchRequest request, @Param("ids") List<String> ids, @Param("userId") String userId);

    @BaseConditionFilter
    List<ModuleCountDTO> countModuleIdByKeywordAndFileType(@Param("request") CaseReviewPageRequest request);

    @BaseConditionFilter
    long caseCount(@Param("request") CaseReviewPageRequest request);


    String getReviewPassRule(@Param("id") String id);

    List<ProjectCountDTO> projectReviewCount(@Param("projectIds") Set<String> projectIds, @Param("startTime") Long startTime, @Param("endTime") Long endTime, @Param("userId") String userId);

    List<ProjectUserCreateCount> userCreateReviewCount(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime, @Param("userIds") Set<String> userIds);

    /**
     * 获取各种状态总数量的评审
     * @param projectId 项目ID
     * @param startTime 时间过滤条件
     * @param endTime 时间过滤条件
     * @return ProjectUserStatusCountDTO userId 在这里不返回
     */
    List<ProjectUserStatusCountDTO> statusReviewCount(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

}
