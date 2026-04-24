package io.vanguard.testops.project.mapper;

import io.vanguard.testops.project.domain.FileAssociation;
import io.vanguard.testops.project.domain.FileAssociationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FileAssociationMapper {
    long countByExample(FileAssociationExample example);

    int deleteByExample(FileAssociationExample example);

    int deleteByPrimaryKey(String id);

    int insert(FileAssociation record);

    int insertSelective(FileAssociation record);

    List<FileAssociation> selectByExample(FileAssociationExample example);

    FileAssociation selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") FileAssociation record, @Param("example") FileAssociationExample example);

    int updateByExample(@Param("record") FileAssociation record, @Param("example") FileAssociationExample example);

    int updateByPrimaryKeySelective(FileAssociation record);

    int updateByPrimaryKey(FileAssociation record);

    int batchInsert(@Param("list") List<FileAssociation> list);

    int batchInsertSelective(@Param("list") List<FileAssociation> list, @Param("selective") FileAssociation.Column ... selective);
}