package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.metadata.domain.MetadataModule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MetadataModule Mapper
 */
public interface MetadataModuleMapper extends BaseMapper<MetadataModule> {
    
    /**
     * 根据ID查询模块（在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型）
     * 只查询未删除的记录（deleted_time IS NULL 表示未删除）
     */
    @Select("SELECT module_id as id, project_id, parent_id, name, module_type, pos, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_module WHERE module_id = #{id} AND deleted_time IS NULL")
    MetadataModule selectByIdWithTimestamp(@Param("id") String id);

    /**
     * 根据项目ID查询模块列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT module_id as id, project_id, parent_id, name, module_type, pos, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_module WHERE project_id = #{projectId} AND deleted_time IS NULL")
    List<MetadataModule> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据项目ID、类型ID和模块类型查询模块列表（仅查询未删除的）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT module_id as id, project_id, type_id, parent_id, name, module_type, pos, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_module " +
            "WHERE project_id = #{projectId} AND type_id = #{typeId} AND module_type = #{moduleType} AND deleted_time IS NULL")
    List<MetadataModule> selectByProjectIdAndTypeId(@Param("projectId") String projectId, 
                                                     @Param("typeId") String typeId, 
                                                     @Param("moduleType") String moduleType);
    
    /**
     * 根据项目ID和模块类型查询模块列表（仅查询未删除的）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT module_id as id, project_id, type_id, parent_id, name, module_type, pos, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_module " +
            "WHERE project_id = #{projectId} AND module_type = #{moduleType} AND deleted_time IS NULL")
    List<MetadataModule> selectByProjectIdAndModuleType(@Param("projectId") String projectId, 
                                                         @Param("moduleType") String moduleType);
    
    /**
     * 批量查询模块：根据项目ID、类型ID列表和模块类型查询模块列表（仅查询未删除的）
     * 用于性能优化，避免 N+1 查询
     */
    @Select("<script>" +
            "SELECT module_id as id, project_id, type_id, parent_id, name, module_type, pos, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_module " +
            "WHERE project_id = #{projectId} AND module_type = #{moduleType} AND deleted_time IS NULL " +
            "AND type_id IN " +
            "<foreach collection='typeIds' item='typeId' open='(' separator=',' close=')'>" +
            "#{typeId}" +
            "</foreach>" +
            "</script>")
    List<MetadataModule> selectByProjectIdAndTypeIds(@Param("projectId") String projectId,
                                                      @Param("typeIds") List<String> typeIds,
                                                      @Param("moduleType") String moduleType);
}
