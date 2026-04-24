package io.vanguard.testops.project.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtFakeErrorMapper {

    List<String> selectByKeyword(@Param("keyword") String keyword);
}
