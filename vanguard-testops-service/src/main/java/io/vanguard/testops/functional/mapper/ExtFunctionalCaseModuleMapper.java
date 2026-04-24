package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.FunctionalCaseModule;
import io.vanguard.testops.functional.dto.FunctionalCaseModuleDTO;
import io.vanguard.testops.functional.dto.ProjectOptionDTO;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.request.AssociateCaseModuleRequest;
import io.vanguard.testops.system.dto.sdk.BaseModule;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFunctionalCaseModuleMapper {
    List<BaseTreeNode> selectBaseByProjectId(@Param("projectId")String projectId);

    List<FunctionalCaseModuleDTO> selectBaseByProjectIdAndReviewId(@Param("reviewId")String reviewId);

    List<ProjectOptionDTO> selectFunRootIdByReviewId(@Param("reviewId")String reviewId);

    List<BaseTreeNode> selectBaseByIds(@Param("ids") List<String> ids);

    List<String> selectChildrenIdsByParentIds(@Param("ids") List<String> deleteIds);

    List<String> selectChildrenIdsSortByPos(String parentId);

    Long getMaxPosByParentId(String parentId);

    BaseModule selectBaseModuleById(String dragNodeId);

    BaseModule selectModuleByParentIdAndPosOperator(NodeSortQueryParam nodeSortQueryParam);

    List<BaseTreeNode> selectIdAndParentIdByProjectId(String projectId);

    List<BaseTreeNode> selectApiCaseModuleByRequest(@Param("request") AssociateCaseModuleRequest request);

    List<BaseTreeNode> selectApiScenarioModuleByRequest(@Param("request") AssociateCaseModuleRequest request);

    List<String> selectIdByProjectIdAndReviewId(@Param("projectId")String projectId, @Param("reviewId")String reviewId);

    void batchUpdateStringColumn(@Param("column") String column, @Param("ids") List<String> ids, @Param("value") String value);

    List<FunctionalCaseModule> getNameInfoByIds(@Param("ids") List<String> ids);
}
