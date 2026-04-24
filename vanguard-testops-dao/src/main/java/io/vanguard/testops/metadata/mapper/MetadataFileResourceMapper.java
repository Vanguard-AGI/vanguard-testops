package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.metadata.domain.MetadataFileResource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MetadataFileResource Mapper
 */
public interface MetadataFileResourceMapper extends BaseMapper<MetadataFileResource> {
    
    /**
     * 根据ID查询文件资源（在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型）
     * 只查询未删除的记录（deleted_time IS NULL 表示未删除）
     */
    @Select("SELECT file_id as id, project_id, storage_name, storage_type, path, file_size, extension, " +
            "content_type, checksum, category, create_user, create_time, " +
            "IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_file_resource " +
            "WHERE file_id = #{id} AND deleted_time IS NULL")
    MetadataFileResource selectByIdWithTimestamp(@Param("id") String id);
    
    /**
     * 根据项目ID查询文件资源列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT file_id as id, project_id, storage_name, storage_type, path, file_size, extension, " +
            "content_type, checksum, category, create_user, create_time, " +
            "IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_file_resource " +
            "WHERE project_id = #{projectId} AND deleted_time IS NULL")
    List<MetadataFileResource> selectByProjectId(@Param("projectId") String projectId);
    
    /**
     * 根据存储类型查询文件资源列表（仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT file_id as id, project_id, storage_name, storage_type, path, file_size, extension, " +
            "content_type, checksum, category, create_user, create_time, " +
            "IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_file_resource " +
            "WHERE storage_type = #{storageType} AND deleted_time IS NULL")
    List<MetadataFileResource> selectByStorageType(@Param("storageType") String storageType);
    
    /**
     * 根据校验和查询文件资源（用于防止重复上传，仅查询未删除的，deleted_time IS NULL 表示未删除）
     * 在 SELECT 中将 deleted_time 转换为时间戳，以便正确映射到 Long 类型
     */
    @Select("SELECT file_id as id, project_id, storage_name, storage_type, path, file_size, extension, " +
            "content_type, checksum, category, create_user, create_time, " +
            "IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.metadata_file_resource " +
            "WHERE checksum = #{checksum} AND deleted_time IS NULL")
    List<MetadataFileResource> selectByChecksum(@Param("checksum") String checksum);
}

