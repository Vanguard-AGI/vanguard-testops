package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作流定义 Mapper 接口
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinition> {

    /**
     * 根据项目ID查询工作流列表（排除已删除）
     */
    @Select("SELECT * FROM workflow_definition WHERE project_id = #{projectId} AND deleted_time IS NULL ORDER BY update_time DESC")
    List<WorkflowDefinition> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据模块ID查询工作流列表（排除已删除）
     */
    @Select("SELECT * FROM workflow_definition WHERE module_id = #{moduleId} AND deleted_time IS NULL ORDER BY update_time DESC")
    List<WorkflowDefinition> selectByModuleId(@Param("moduleId") String moduleId);

    /**
     * 根据ID查询工作流（排除已删除）
     */
    @Select("SELECT * FROM workflow_definition WHERE workflow_id = #{workflowId} AND deleted_time IS NULL")
    WorkflowDefinition selectByWorkflowId(@Param("workflowId") String workflowId);
    
    /**
     * 批量查询工作流：根据模块ID列表查询工作流列表（排除已删除）
     * 用于性能优化，避免 N+1 查询
     */
    @Select("<script>" +
            "SELECT * FROM workflow_definition " +
            "WHERE deleted_time IS NULL AND module_id IN " +
            "<foreach collection='moduleIds' item='moduleId' open='(' separator=',' close=')'>" +
            "#{moduleId}" +
            "</foreach>" +
            "ORDER BY update_time DESC" +
            "</script>")
    List<WorkflowDefinition> selectByModuleIds(@Param("moduleIds") List<String> moduleIds);
}

