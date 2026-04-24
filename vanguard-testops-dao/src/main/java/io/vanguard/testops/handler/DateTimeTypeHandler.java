package io.vanguard.testops.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Long 时间戳（毫秒）与 MySQL datetime 类型转换的 TypeHandler
 * 用于处理 deleted_time 字段
 */
public class DateTimeTypeHandler implements TypeHandler<Long> {

    // 默认未删除时间：1970-01-01 00:00:00 对应的时间戳（毫秒）
    private static final long DEFAULT_DELETED_TIME = 0L;

    @Override
    public void setParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            // 如果为 null，设置为默认值（未删除）
            ps.setTimestamp(i, new Timestamp(DEFAULT_DELETED_TIME));
        } else {
            // 将 Long 时间戳（毫秒）转换为 Timestamp
            ps.setTimestamp(i, new Timestamp(parameter));
        }
    }

    @Override
    public Long getResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.getTime();
    }

    @Override
    public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp == null ? null : timestamp.getTime();
    }

    @Override
    public Long getResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return timestamp == null ? null : timestamp.getTime();
    }
}

