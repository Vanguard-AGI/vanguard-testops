package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiDefinitionModule;
import io.vanguard.testops.api.dto.debug.ApiTreeNode;
import io.vanguard.testops.api.dto.definition.ApiModuleRequest;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.system.dto.sdk.BaseModule;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApiDefinitionModuleMapper {
    List<BaseTreeNode> selectBaseByRequest(@Param("request") ApiModuleRequest request);

    List<BaseTreeNode> selectIdAndParentIdByRequest(@Param("request") ApiModuleRequest request);

    List<String> selectChildrenIdsByParentIds(@Param("ids") List<String> deleteIds);

    List<String> selectChildrenIdsSortByPos(String parentId);

    void deleteByIds(@Param("ids") List<String> deleteId);

    Long getMaxPosByParentId(String parentId);

    BaseModule selectBaseModuleById(String dragNodeId);

    BaseModule selectModuleByParentIdAndPosOperator(NodeSortQueryParam nodeSortQueryParam);

    @BaseConditionFilter
    List<ApiTreeNode> selectApiDataByRequest(@Param("request") ApiModuleRequest request, @Param("deleted") boolean deleted);

    @BaseConditionFilter
    List<ModuleCountDTO> countModuleIdByRequest(@Param("request") ApiModuleRequest request, @Param("deleted") boolean deleted, @Param("isRepeat") boolean isRepeat);

    List<BaseTreeNode> selectNodeByIds(@Param("ids") List<String> ids);

    List<BaseTreeNode> selectBaseByIds(@Param("ids") List<String> ids);

    List<String> getModuleIdsByParentIds(@Param("parentIds") List<String> parentIds);

    List<ApiDefinitionModule> getNameInfoByIds(@Param("ids") List<String> ids);

    /**
     * 获取ApiCase的模块count
     * @param request
     * @param deleted
     * @param isRepeat
     * @return
     */
    List<ModuleCountDTO> apiCaseCountModuleIdByRequest(@Param("request") ApiModuleRequest request, @Param("deleted") boolean deleted, @Param("isRepeat") boolean isRepeat);
}
