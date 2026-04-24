package io.vanguard.testops.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.metadata.domain.ScriptManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

/**
 * ScriptManage Mapper
 */
@Mapper
public interface ScriptManageMapper extends BaseMapper<ScriptManage> {
    
    @Results({
        @Result(column = "script_id", property = "scriptId"),
        @Result(column = "script_name", property = "scriptName"),
        @Result(column = "script_type", property = "scriptType"),
        @Result(column = "script_content", property = "scriptContent"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "deleted_time", property = "deletedTime", jdbcType = JdbcType.BIGINT)
    })
    @Select("SELECT script_id, script_name, script_type, script_content, " +
            "create_time, update_time, IFNULL(UNIX_TIMESTAMP(deleted_time) * 1000, NULL) as deleted_time " +
            "FROM spotter_aegis.script_manage WHERE script_id = #{scriptId} AND deleted_time IS NULL")
    ScriptManage selectByIdWithTimestamp(@Param("scriptId") String scriptId);
}

