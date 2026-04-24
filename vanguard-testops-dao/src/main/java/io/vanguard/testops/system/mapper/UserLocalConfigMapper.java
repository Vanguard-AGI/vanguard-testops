package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.UserLocalConfig;
import io.vanguard.testops.system.domain.UserLocalConfigExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserLocalConfigMapper {
    long countByExample(UserLocalConfigExample example);

    int deleteByExample(UserLocalConfigExample example);

    int deleteByPrimaryKey(String id);

    int insert(UserLocalConfig record);

    int insertSelective(UserLocalConfig record);

    List<UserLocalConfig> selectByExample(UserLocalConfigExample example);

    UserLocalConfig selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") UserLocalConfig record, @Param("example") UserLocalConfigExample example);

    int updateByExample(@Param("record") UserLocalConfig record, @Param("example") UserLocalConfigExample example);

    int updateByPrimaryKeySelective(UserLocalConfig record);

    int updateByPrimaryKey(UserLocalConfig record);

    int batchInsert(@Param("list") List<UserLocalConfig> list);

    int batchInsertSelective(@Param("list") List<UserLocalConfig> list, @Param("selective") UserLocalConfig.Column ... selective);
}