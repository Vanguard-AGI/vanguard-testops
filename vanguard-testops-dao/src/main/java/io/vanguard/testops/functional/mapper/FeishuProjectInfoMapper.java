package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.FeishuProjectInfo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FeishuProjectInfoMapper {
    int insert(FeishuProjectInfo record);

    int updateByPrimaryKey(FeishuProjectInfo record);

    FeishuProjectInfo selectByPrimaryKey(String id);

    FeishuProjectInfo selectByFeishuProjectId(String feishuProjectId);

    List<FeishuProjectInfo> selectByProjectId(String projectId);
}
