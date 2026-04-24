package io.vanguard.testops.project.mapper;

import io.vanguard.testops.project.domain.FileMetadata;
import io.vanguard.testops.project.domain.FileMetadataExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FileMetadataMapper {
    long countByExample(FileMetadataExample example);

    int deleteByExample(FileMetadataExample example);

    int deleteByPrimaryKey(String id);

    int insert(FileMetadata record);

    int insertSelective(FileMetadata record);

    List<FileMetadata> selectByExample(FileMetadataExample example);

    FileMetadata selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") FileMetadata record, @Param("example") FileMetadataExample example);

    int updateByExample(@Param("record") FileMetadata record, @Param("example") FileMetadataExample example);

    int updateByPrimaryKeySelective(FileMetadata record);

    int updateByPrimaryKey(FileMetadata record);

    int batchInsert(@Param("list") List<FileMetadata> list);

    int batchInsertSelective(@Param("list") List<FileMetadata> list, @Param("selective") FileMetadata.Column ... selective);
}