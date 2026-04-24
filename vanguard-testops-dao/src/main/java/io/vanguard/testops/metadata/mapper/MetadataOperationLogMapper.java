package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.metadata.domain.MetadataOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetadataOperationLogMapper extends BaseMapper<MetadataOperationLog> {
}

