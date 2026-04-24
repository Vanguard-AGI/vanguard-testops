package io.vanguard.testops.sdk.mapper;

import io.vanguard.testops.sdk.domain.OperationLogBlob;
import io.vanguard.testops.sdk.domain.OperationLogBlobExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OperationLogBlobMapper {
    long countByExample(OperationLogBlobExample example);

    int deleteByExample(OperationLogBlobExample example);

    int deleteByPrimaryKey(Long id);

    int insert(OperationLogBlob record);

    int insertSelective(OperationLogBlob record);

    List<OperationLogBlob> selectByExampleWithBLOBs(OperationLogBlobExample example);

    List<OperationLogBlob> selectByExample(OperationLogBlobExample example);

    OperationLogBlob selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") OperationLogBlob record, @Param("example") OperationLogBlobExample example);

    int updateByExampleWithBLOBs(@Param("record") OperationLogBlob record, @Param("example") OperationLogBlobExample example);

    int updateByExample(@Param("record") OperationLogBlob record, @Param("example") OperationLogBlobExample example);

    int updateByPrimaryKeySelective(OperationLogBlob record);

    int updateByPrimaryKeyWithBLOBs(OperationLogBlob record);

    int batchInsert(@Param("list") List<OperationLogBlob> list);

    int batchInsertSelective(@Param("list") List<OperationLogBlob> list, @Param("selective") OperationLogBlob.Column ... selective);
}