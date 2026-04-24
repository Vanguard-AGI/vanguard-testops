package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.dto.debug.ApiDebugRequest;
import io.vanguard.testops.api.dto.debug.ApiTreeNode;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.system.dto.sdk.BaseModule;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApiDebugModuleMapper {
    List<BaseTreeNode> selectBaseByProtocolAndUser(String userId);

    List<BaseTreeNode> selectIdAndParentIdByProtocolAndUserId(String userId);

    List<String> selectChildrenIdsByParentIds(@Param("ids") List<String> deleteIds);

    List<BaseTreeNode> selectBaseNodeByIds(@Param("ids") List<String> ids);

    List<String> selectChildrenIdsSortByPos(String parentId);

    void deleteByIds(@Param("ids") List<String> deleteId);

    Long getMaxPosByParentId(String parentId);

    BaseModule selectBaseModuleById(String dragNodeId);

    BaseModule selectModuleByParentIdAndPosOperator(NodeSortQueryParam nodeSortQueryParam);

    List<ApiTreeNode> selectApiDebugByProtocolAndUser(String userId);

    List<ModuleCountDTO> countModuleIdByKeywordAndProtocol(@Param("request") ApiDebugRequest request, @Param("userId") String userId);
}
