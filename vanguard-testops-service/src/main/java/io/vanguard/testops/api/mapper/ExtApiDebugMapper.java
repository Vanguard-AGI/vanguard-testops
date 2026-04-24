package io.vanguard.testops.api.mapper;


import io.vanguard.testops.api.dto.debug.ApiDebugSimpleDTO;
import io.vanguard.testops.project.dto.DropNode;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 * @date : 2026-04-22
 */
@Mapper
public interface ExtApiDebugMapper {
    List<ApiDebugSimpleDTO> list(@Param("protocol") String protocol, @Param("userId") String userId);

    Long getPos(@Param("userId") String userId);

    Long getPrePos(@Param("userId") String userId, @Param("basePos") Long basePos);

    Long getLastPos(@Param("userId") String userId, @Param("basePos") Long basePos);

    void updatePos(String id, long pos);

    List<String> selectIdByProjectIdOrderByPos(String userId);

    DropNode selectDragInfoById(String id);

    DropNode selectNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);
}