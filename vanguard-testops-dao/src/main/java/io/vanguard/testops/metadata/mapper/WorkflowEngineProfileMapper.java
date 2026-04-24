package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.vanguard.testops.metadata.domain.WorkflowEngineProfile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

/**
 * WorkflowEngineProfile Mapper
 */
public interface WorkflowEngineProfileMapper extends BaseMapper<WorkflowEngineProfile> {
    
    /**
     * 根据项目ID查询环境配置列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT environment_id as id, project_id, environment_name as name, engine_type, env_code, robots, " +
            "data_endpoint, variables, domain, xxljob_info, mq_info, dubbo_info, create_user, update_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM workflow_engine_profile " +
            "WHERE project_id = #{projectId} AND deleted_time IS NULL " +
            "ORDER BY create_time DESC")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "project_id", property = "projectId"),
        @Result(column = "name", property = "name"),
        @Result(column = "engine_type", property = "engineType"),
        @Result(column = "env_code", property = "envCode"),
        @Result(column = "robots", property = "robots", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "data_endpoint", property = "dataEndpoint", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "variables", property = "variables", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "domain", property = "domain"),
        @Result(column = "xxljob_info", property = "xxljobInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "mq_info", property = "mqInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "dubbo_info", property = "dubboInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "create_user", property = "createUser"),
        @Result(column = "update_user", property = "updateUser"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "deleted_time", property = "deletedTime", jdbcType = JdbcType.BIGINT)
    })
    List<WorkflowEngineProfile> selectByProjectId(@Param("projectId") String projectId);
    
    /**
     * 根据ID查询环境配置（在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型）
     * 只查询未删除的记录（deleted_time IS NULL 表示未删除）
     */
    @Select("SELECT environment_id as id, project_id, environment_name as name, engine_type, env_code, robots, " +
            "data_endpoint, variables, domain, xxljob_info, mq_info, dubbo_info, create_user, update_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM workflow_engine_profile " +
            "WHERE environment_id = #{id} AND deleted_time IS NULL")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "project_id", property = "projectId"),
        @Result(column = "name", property = "name"),
        @Result(column = "engine_type", property = "engineType"),
        @Result(column = "env_code", property = "envCode"),
        @Result(column = "robots", property = "robots", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "data_endpoint", property = "dataEndpoint", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "variables", property = "variables", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "domain", property = "domain"),
        @Result(column = "xxljob_info", property = "xxljobInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "mq_info", property = "mqInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "dubbo_info", property = "dubboInfo", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "create_user", property = "createUser"),
        @Result(column = "update_user", property = "updateUser"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "deleted_time", property = "deletedTime", jdbcType = JdbcType.BIGINT)
    })
    WorkflowEngineProfile selectByIdWithTimestamp(@Param("id") String id);
}

