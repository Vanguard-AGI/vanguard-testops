package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.vanguard.testops.metadata.domain.MetadataDefinition;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

/**
 * MetadataDefinition Mapper
 */
public interface MetadataDefinitionMapper extends BaseMapper<MetadataDefinition> {
    
    /**
     * 根据项目ID查询元数据定义列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳（毫秒），以便正确映射到 Long 类型
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition WHERE project_id = #{projectId} AND deleted_time IS NULL " +
            "ORDER BY create_time DESC")
    List<MetadataDefinition> selectByProjectId(@Param("projectId") String projectId);
    
    /**
     * 根据项目ID和关键字查询元数据定义列表（支持按name模糊搜索）
     * 仅查询未删除的，deleted_time IS NULL 表示未删除
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition " +
            "WHERE project_id = #{projectId} AND deleted_time IS NULL " +
            "AND (name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY create_time DESC")
    List<MetadataDefinition> selectByProjectIdAndKeyword(@Param("projectId") String projectId, @Param("keyword") String keyword);
    
    /**
     * 根据项目ID和协议类型查询元数据定义列表
     * 仅查询未删除的，deleted_time IS NULL 表示未删除
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition " +
            "WHERE project_id = #{projectId} AND protocol = #{protocol} AND deleted_time IS NULL " +
            "ORDER BY create_time DESC")
    List<MetadataDefinition> selectByProjectIdAndProtocol(@Param("projectId") String projectId, @Param("protocol") String protocol);
    
    /**
     * 根据项目ID、协议类型和关键字查询元数据定义列表
     * 仅查询未删除的，deleted_time IS NULL 表示未删除
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition " +
            "WHERE project_id = #{projectId} AND protocol = #{protocol} AND deleted_time IS NULL " +
            "AND (name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY create_time DESC")
    List<MetadataDefinition> selectByProjectIdAndProtocolAndKeyword(@Param("projectId") String projectId, @Param("protocol") String protocol, @Param("keyword") String keyword);
    
    /**
     * 根据模块ID查询元数据定义列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳（毫秒），以便正确映射到 Long 类型
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition WHERE module_id = #{moduleId} AND deleted_time IS NULL")
    List<MetadataDefinition> selectByModuleId(@Param("moduleId") String moduleId);
    
    /**
     * 根据ID查询元数据定义（在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型）
     * 只查询未删除的记录（deleted_time IS NULL 表示未删除）
     */
    @Results({
        @Result(column = "request_config", property = "requestConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "response_config", property = "responseConfig", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class),
        @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR, typeHandler = JacksonTypeHandler.class)
    })
    @Select("SELECT definition_id as id, project_id, module_id, name, protocol, version, is_latest, is_case, description, " +
            "request_config, response_config, script_content, tags, create_user, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_definition WHERE definition_id = #{id} AND deleted_time IS NULL")
    MetadataDefinition selectByIdWithTimestamp(@Param("id") String id);
    
    /**
     * 统计项目下每个模块的元数据定义数量（仅统计未删除的，deleted_time IS NULL 表示未删除）
     */
    @Select("SELECT module_id as moduleId, COUNT(*) as dataCount " +
            "FROM spotter_aegis.metadata_definition " +
            "WHERE project_id = #{projectId} AND deleted_time IS NULL " +
            "GROUP BY module_id")
    List<ModuleCountDTO> countByModuleId(@Param("projectId") String projectId);
}
