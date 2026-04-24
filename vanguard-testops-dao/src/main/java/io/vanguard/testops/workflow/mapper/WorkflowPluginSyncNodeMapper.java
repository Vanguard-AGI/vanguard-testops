package io.vanguard.testops.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.vanguard.testops.workflow.domain.WorkflowPluginSyncNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 外部插件同步节点数据 Mapper 接口
 */
@Mapper
public interface WorkflowPluginSyncNodeMapper extends BaseMapper<WorkflowPluginSyncNode> {

    /**
     * 根据用户邮箱查询节点列表（排除已删除）
     * @param email 用户邮箱
     * @return 节点列表
     */
    List<WorkflowPluginSyncNode> selectByEmail(@Param("email") String email);

    /**
     * 根据用户邮箱和节点类型查询节点列表（排除已删除）
     * @param email 用户邮箱
     * @param nodeType 节点类型
     * @return 节点列表
     */
    List<WorkflowPluginSyncNode> selectByEmailAndType(@Param("email") String email, @Param("nodeType") String nodeType);
}

