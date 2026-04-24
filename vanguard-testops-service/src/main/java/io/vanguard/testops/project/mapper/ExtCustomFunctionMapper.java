package io.vanguard.testops.project.mapper;

import io.vanguard.testops.project.dto.customfunction.CustomFunctionDTO;
import io.vanguard.testops.project.dto.customfunction.request.CustomFunctionPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
public interface ExtCustomFunctionMapper {
    List<CustomFunctionDTO> list(@Param("request") CustomFunctionPageRequest request);
}
