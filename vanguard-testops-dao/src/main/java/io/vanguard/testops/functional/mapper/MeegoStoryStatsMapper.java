package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.MeegoStoryStats;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MeegoStoryStatsMapper {
    int insert(MeegoStoryStats record);

    MeegoStoryStats selectByPrimaryKey(Long id);

    MeegoStoryStats selectByStoryId(@Param("storyId") String storyId);

    List<MeegoStoryStats> selectByStoryNameLike(@Param("keyword") String keyword);

    List<MeegoStoryStats> selectAll();

    int updateByPrimaryKey(MeegoStoryStats record);
}
