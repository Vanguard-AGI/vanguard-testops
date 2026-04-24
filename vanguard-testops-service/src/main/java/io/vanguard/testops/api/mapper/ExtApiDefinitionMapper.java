package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiDefinition;
import io.vanguard.testops.api.domain.ApiDefinitionCustomField;
import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.api.dto.ApiDefinitionExecuteInfo;
import io.vanguard.testops.api.dto.ReferenceDTO;
import io.vanguard.testops.api.dto.ReferenceRequest;
import io.vanguard.testops.api.dto.converter.ApiDefinitionDetail;
import io.vanguard.testops.api.dto.definition.*;
import io.vanguard.testops.api.dto.scenario.ScenarioSystemRequest;
import io.vanguard.testops.project.dto.DropNode;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.project.dto.ProjectCountDTO;
import io.vanguard.testops.project.dto.ProjectUserCreateCount;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ExtApiDefinitionMapper {
    void deleteApiToGc(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("time") long time);

    @BaseConditionFilter
    List<ApiDefinitionDTO> list(@Param("request") ApiDefinitionPageRequest request);

    @BaseConditionFilter
    List<ApiDefinitionDTO> listDoc(@Param("request") ApiDefinitionDocRequest request);

    List<ApiTestCase> selectNotInTrashCaseIdsByApiIds(@Param("apiIds") List<String> apiIds);

    Long getPos(@Param("projectId") String projectId);

    @BaseConditionFilter
    List<String> getIds(@Param("request") TableBatchProcessDTO request, @Param("projectId") String projectId, @Param("protocols") List<String> protocols, @Param("deleted") boolean deleted, @Param("includeIds") List<String> includeIds, @Param("excludeIds") List<String> excludeIds);

    @BaseConditionFilter
    List<String> getIdsBySort(@Param("request") TableBatchProcessDTO request, @Param("projectId") String projectId, @Param("protocols") List<String> protocols, @Param("orderColumns") String orderColumns, @Param("deleted") boolean deleted);

    List<String> getRefIds(@Param("ids") List<String> ids, @Param("deleted") boolean deleted);

    List<String> getIdsByRefId(@Param("refIds") List<String> refIds, @Param("deleted") boolean deleted);

    List<ApiDefinition> getTagsByIds(@Param("ids") List<String> ids, @Param("deleted") boolean deleted);

    List<ApiDefinitionVersionDTO> getApiDefinitionByRefId(@Param("refId") String refId);

    @BaseConditionFilter
    void batchMove(@Param("request") ApiDefinitionBatchMoveRequest request, @Param("ids") List<String> ids, @Param("userId") String userId);

    void batchDeleteByRefId(@Param("refIds") List<String> refIds, @Param("userId") String userId, @Param("projectId") String projectId);

    void batchDeleteById(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("projectId") String projectId);

    void batchRecoverById(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("projectId") String projectId);

    void clearLatestVersion(@Param("refId") String refId, @Param("projectId") String projectId);

    void updateLatestVersion(@Param("id") String id, @Param("projectId") String projectId);

    @BaseConditionFilter
    List<ApiDefinitionDetail> importList(@Param("request") ApiDefinitionPageRequest request);

    List<String> selectIdsByIdsAndDeleted(@Param("ids") List<String> ids, @Param("deleted") boolean deleted);

    List<String> selectByProjectId(@Param("projectId") String projectId);

    List<OptionDTO> selectVersionOptionByIds(@Param("apiIds") List<String> apiIds);

    ApiDefinition selectApiDefinitionByVersion(@Param("refId") String refId, @Param("versionId") String versionId);

    List<ApiDefinitionCustomField> getCustomFieldByCaseIds(@Param("ids") List<String> ids);

    List<String> getIdsByModules(@Param("request") ScenarioSystemRequest request);

    Long getPrePos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    Long getLastPos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    List<ApiDefinitionExecuteInfo> getApiDefinitionExecuteInfo(@Param("ids") List<String> ids);

    ApiDefinition selectByProjectNumAndApiNum(@Param("projectNum") String projectNum, @Param("apiNum") String apiNum);

    void updatePos(String id, long pos);

    List<String> selectIdByProjectIdOrderByPos(String projectId);

    DropNode selectDragInfoById(String id);

    DropNode selectNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);

    List<ReferenceDTO> getReference(@Param("request") ReferenceRequest request);

    List<ApiDefinition> selectByProjectNum(String projectNum);

    List<ApiDefinitionWithBlob> selectApiDefinitionWithBlob(@Param("ids") List<String> ids);

    List<ApiDefinition> selectAllApi(@Param("projectId") String projectId, @Param("protocols") List<String> protocols);


    List<ApiDefinition> getListBySelectModules(@Param("projectId") String projectId, @Param("moduleIds") List<String> moduleIds, @Param("protocols") List<String> protocols);

    List<ApiDefinition> getListBySelectIds(@Param("projectId") String projectId, @Param("ids") List<String> ids, @Param("protocols") List<String> protocols);

    List<String> getIdsByShareParam(@Param("projectId") String projectId, @Param("condition") String condition);

    long countByProjectAndId(@Param("projectId") String projectId, @Param("id") String id);

    Long selectNumById(String id);

    List<ProjectCountDTO> projectApiCount(@Param("projectIds") Set<String> projectIds, @Param("startTime") Long startTime, @Param("endTime") Long endTime, @Param("userId") String userId);

    List<ProjectUserCreateCount> userCreateApiCount(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime, @Param("userIds") Set<String> userIds);

    /**
     * 获取根据时间过滤有更新的api
     *
     * @param projectId xiangmuid
     * @param startTime 时间过滤条件
     * @param endTime   时间过滤条件
     * @return List<ApiDefinitionUpdateDTO>
     */
    List<ApiDefinitionUpdateDTO> getUpdateApiList(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    List<ApiRefSourceCountDTO> scenarioRefApiCount(@Param("projectId") String projectId, @Param("resourceIds") List<String> resourceIds);

    List<ApiDefinition> getCreateApiList(@Param("projectId") String projectId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    List<ApiDefinition> selectBaseInfoByProjectId(@Param("projectId") String projectId, @Param("protocols") List<String> protocols, @Param("ignoreApiIds") List<String> ignoreApiIds);
}
