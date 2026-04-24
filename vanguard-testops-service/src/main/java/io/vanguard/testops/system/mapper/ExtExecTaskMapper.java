package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.vanguard.testops.system.dto.taskhub.ExecTaskItemDetailDTO;
import io.vanguard.testops.system.dto.taskhub.TaskHubDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtExecTaskMapper {
    List<TaskHubDTO> selectList(@Param("request") BasePageRequest request, @Param("orgId") String orgId, @Param("projectId") String projectId);

    void deleteTaskByIds(@Param("ids") List<String> ids, @Param("orgId") String orgId, @Param("projectId") String projectId);

    List<String> getIds(@Param("request") TableBatchProcessDTO request, @Param("organizationId") String organizationId, @Param("projectId") String projectId, @Param("flag") boolean flag);

    void batchUpdateTaskStatus(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("organizationId") String organizationId, @Param("projectId") String projectId, @Param("status") String status);

    /**
     * 查询时间范围内的任务ID集合
     *
     * @param timeMills 时间戳
     * @param projectId 项目ID
     * @return 任务ID列表
     */
    List<String> getTaskIdsByTime(@Param("timeMills") long timeMills, @Param("projectId") String projectId);

    List<ExecTaskItemDetailDTO> selectTypeByItemId(@Param("itemId") String itemId);

    List<String> getSelectIds(@Param("ids") List<String> ids, @Param("flag") boolean flag);
}
