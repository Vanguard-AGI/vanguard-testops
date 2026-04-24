package io.vanguard.testops.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-12
 */
public interface ExtStatusItemMapper {
    List<String> getStatusItemIdByRefId(@Param("refId") String refId);
}
