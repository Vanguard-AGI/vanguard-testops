package io.vanguard.testops.project.mapper;

import io.vanguard.testops.project.dto.DropNode;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.sdk.domain.Environment;
import io.vanguard.testops.sdk.domain.EnvironmentGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtEnvironmentMapper {

    List<Environment> selectByKeyword(@Param("keyword") String keyword, @Param("selectId") boolean selectId , @Param("projectId") String projectId);

    List<EnvironmentGroup> groupList(@Param("keyword") String keyword, @Param("projectId") String projectId);

    Long getPos(String projectId);

    Long getPrePos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    Long getLastPos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    Long getGroupPos(String projectId);

    Long getGroupPrePos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    Long getGroupLastPos(@Param("projectId") String projectId, @Param("basePos") Long basePos);

    DropNode selectDragInfoById(String id);

    DropNode selectNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);

    List<String> selectIdByProjectIdOrderByPos(String projectId);
    long updatePos(@Param("id") String id, @Param("pos") long pos);

    void updateGroupPos(String id, long pos);

    List<String> selectGroupIdByProjectIdOrderByPos(String projectId);

    DropNode selectGroupDragInfoById(String id);

    DropNode selectGroupNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);
}
