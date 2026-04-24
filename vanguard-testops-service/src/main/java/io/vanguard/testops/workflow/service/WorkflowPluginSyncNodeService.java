package io.vanguard.testops.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.workflow.domain.WorkflowPluginSyncNode;
import io.vanguard.testops.workflow.dto.PluginSyncNodeDTO;
import io.vanguard.testops.workflow.dto.PluginSyncRequest;
import io.vanguard.testops.workflow.dto.PluginSyncResponse;
import io.vanguard.testops.workflow.mapper.WorkflowPluginSyncNodeMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vanguard.testops.sdk.util.LogUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 外部插件同步节点数据服务
 */
@Service
public class WorkflowPluginSyncNodeService {

    @Resource
    private WorkflowPluginSyncNodeMapper pluginSyncNodeMapper;

    /**
     * 根据用户邮箱查询节点列表
     * @param email 用户邮箱
     * @return 节点列表
     */
    public List<PluginSyncNodeDTO> getNodesByEmail(String email) {
        List<WorkflowPluginSyncNode> nodes = pluginSyncNodeMapper.selectByEmail(email);
        return nodes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 根据用户邮箱和节点类型查询节点列表
     * @param email 用户邮箱
     * @param nodeType 节点类型（HTTP/SQL/DUBBO/ROCKETMQ）
     * @return 节点列表
     */
    public List<PluginSyncNodeDTO> getNodesByEmailAndType(String email, String nodeType) {
        List<WorkflowPluginSyncNode> nodes = pluginSyncNodeMapper.selectByEmailAndType(email, nodeType);
        return nodes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 同步节点数据
     * @param request 同步请求
     * @return 同步响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PluginSyncResponse syncNodes(PluginSyncRequest request) {
        PluginSyncResponse response = new PluginSyncResponse();
        response.setSuccess(true);
        response.setSuccessCount(0);
        response.setFailCount(0);
        List<PluginSyncResponse.FailItem> failItems = new ArrayList<>();

        String email = request.getEmail();
        List<PluginSyncRequest.EndpointData> endpoints = request.getEndpoints();

        if (endpoints == null || endpoints.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("节点列表不能为空");
            return response;
        }

        long currentTime = System.currentTimeMillis();

        // 保存新数据（每次同步都新增，不更新，不删除）
        for (int i = 0; i < endpoints.size(); i++) {
            PluginSyncRequest.EndpointData endpoint = endpoints.get(i);
            try {
                // 验证节点类型
                String nodeType = endpoint.getType();
                if (StringUtils.isBlank(nodeType) || (!"HTTP".equalsIgnoreCase(nodeType) && !"SQL".equalsIgnoreCase(nodeType) 
                        && !"DUBBO".equalsIgnoreCase(nodeType) && !"ROCKETMQ".equalsIgnoreCase(nodeType))) {
                    throw new IllegalArgumentException("节点类型必须是 HTTP、SQL、DUBBO 或 ROCKETMQ");
                }

                // 验证节点数据
                if ("HTTP".equalsIgnoreCase(nodeType)) {
                    if (StringUtils.isBlank(endpoint.getUrl()) && StringUtils.isBlank(endpoint.getPath())) {
                        throw new IllegalArgumentException("HTTP 节点必须包含 url 或 path");
                    }
                } else if ("SQL".equalsIgnoreCase(nodeType)) {
                    if (StringUtils.isBlank(endpoint.getSql())) {
                        throw new IllegalArgumentException("SQL 节点必须包含 sql 语句");
                    }
                } else if ("DUBBO".equalsIgnoreCase(nodeType)) {
                    if (StringUtils.isBlank(endpoint.getInterfaceName()) || StringUtils.isBlank(endpoint.getMethodName())) {
                        throw new IllegalArgumentException("DUBBO 节点必须包含 interfaceName 和 methodName");
                    }
                } else if ("ROCKETMQ".equalsIgnoreCase(nodeType)) {
                    if (StringUtils.isBlank(endpoint.getTopic())) {
                        throw new IllegalArgumentException("ROCKETMQ 节点必须包含 topic");
                    }
                }

                // 构建节点数据 Map
                Map<String, Object> endpointData = new HashMap<>();
                if ("HTTP".equalsIgnoreCase(nodeType)) {
                    endpointData.put("method", endpoint.getMethod());
                    endpointData.put("url", endpoint.getUrl());
                    endpointData.put("path", endpoint.getPath());
                    endpointData.put("headers", endpoint.getHeaders());
                    endpointData.put("queryParams", endpoint.getQueryParams());
                    endpointData.put("json", endpoint.getJson());
                    endpointData.put("data", endpoint.getData());
                    endpointData.put("body", endpoint.getBody());
                } else if ("SQL".equalsIgnoreCase(nodeType)) {
                    endpointData.put("sql", endpoint.getSql());
                    endpointData.put("operationType", endpoint.getOperationType());
                } else if ("DUBBO".equalsIgnoreCase(nodeType)) {
                    endpointData.put("interfaceName", endpoint.getInterfaceName());
                    endpointData.put("methodName", endpoint.getMethodName());
                    endpointData.put("params", endpoint.getParams());
                    endpointData.put("paramTypes", endpoint.getParamTypes());
                    endpointData.put("group", endpoint.getGroup());
                    endpointData.put("version", endpoint.getVersion());
                    endpointData.put("timeout", endpoint.getTimeout());
                    endpointData.put("url", endpoint.getDubboUrl());
                    endpointData.put("applicationName", endpoint.getApplicationName());
                    endpointData.put("siteTenant", endpoint.getSiteTenant());
                    endpointData.put("dubboTag", endpoint.getDubboTag());
                } else if ("ROCKETMQ".equalsIgnoreCase(nodeType)) {
                    endpointData.put("topic", endpoint.getTopic());
                    endpointData.put("tag", endpoint.getTag());
                    // 优先使用messageKey，如果没有则使用key
                    String messageKey = StringUtils.isNotBlank(endpoint.getMessageKey()) 
                        ? endpoint.getMessageKey() 
                        : endpoint.getKey();
                    endpointData.put("key", messageKey);
                    endpointData.put("messageKey", messageKey);
                    endpointData.put("messageBody", endpoint.getMessageBody());
                    // 优先使用nameServer，如果没有则使用mqUrl
                    String nameServer = StringUtils.isNotBlank(endpoint.getNameServer()) 
                        ? endpoint.getNameServer() 
                        : endpoint.getMqUrl();
                    endpointData.put("mqUrl", nameServer);
                    endpointData.put("nameServer", nameServer);
                    endpointData.put("siteTenant", endpoint.getSiteTenant());
                }
                if (endpoint.getExtra() != null) {
                    endpointData.putAll(endpoint.getExtra());
                }

                // 生成节点ID（使用IDGenerator生成唯一ID，每次同步都新增）
                String nodeId = IDGenerator.nextStr();

                // 创建新节点并入库（每次同步都新增，不检查是否存在）
                WorkflowPluginSyncNode node = new WorkflowPluginSyncNode();
                node.setNodeId(nodeId);
                node.setEmail(email);
                node.setNodeType(nodeType.toUpperCase());
                node.setEndpointData(endpointData);
                node.setCreateTime(currentTime);
                node.setUpdateTime(currentTime);
                node.setDeletedTime(null);
                // 入库：插入新记录到数据库
                int insertResult = pluginSyncNodeMapper.insert(node);
                if (insertResult > 0) {
                    LogUtils.info("成功入库新节点: email={}, nodeId={}, nodeType={}", email, nodeId, nodeType);
                } else {
                    throw new RuntimeException("节点入库失败: insert返回0");
                }
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception e) {
                response.setFailCount(response.getFailCount() + 1);
                PluginSyncResponse.FailItem failItem = new PluginSyncResponse.FailItem();
                failItem.setIndex(i);
                failItem.setReason(e.getMessage());
                failItems.add(failItem);
            }
        }

        // 注意：不再自动删除不在新数据中的旧节点
        // 同步逻辑改为增量更新：只新增/更新同步的节点，不删除其他节点
        // 如果需要删除节点，应该通过显式的删除接口或传递删除标记来实现

        response.setFailItems(failItems);
        if (response.getFailCount() > 0) {
            response.setSuccess(false);
            response.setMessage(String.format("同步完成，成功: %d, 失败: %d", response.getSuccessCount(), response.getFailCount()));
            LogUtils.warn(String.format("同步节点数据完成（有失败）: email=%s, 成功=%d, 失败=%d", email, response.getSuccessCount(), response.getFailCount()));
        } else {
            response.setMessage(String.format("同步成功，共 %d 个节点", response.getSuccessCount()));
            LogUtils.info("同步节点数据成功: email={}, 成功数量={}", email, response.getSuccessCount());
        }

        return response;
    }

    /**
     * 生成节点ID（基于内容生成稳定的ID）
     */
    private String generateNodeId(String email, String nodeType, PluginSyncRequest.EndpointData endpoint) {
        try {
            // 构建用于生成ID的关键信息
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(email).append("_").append(nodeType.toUpperCase()).append("_");
            
            if ("HTTP".equalsIgnoreCase(nodeType)) {
                String url = endpoint.getUrl() != null ? endpoint.getUrl() : endpoint.getPath();
                String method = endpoint.getMethod() != null ? endpoint.getMethod().toUpperCase() : "GET";
                keyBuilder.append(method).append("_").append(url);
            } else if ("SQL".equalsIgnoreCase(nodeType)) {
                String sql = endpoint.getSql() != null ? endpoint.getSql() : "";
                keyBuilder.append(sql);
            } else if ("DUBBO".equalsIgnoreCase(nodeType)) {
                String interfaceName = endpoint.getInterfaceName() != null ? endpoint.getInterfaceName() : "";
                String methodName = endpoint.getMethodName() != null ? endpoint.getMethodName() : "";
                keyBuilder.append(interfaceName).append("#").append(methodName);
            } else if ("ROCKETMQ".equalsIgnoreCase(nodeType)) {
                String topic = endpoint.getTopic() != null ? endpoint.getTopic() : "";
                String tag = endpoint.getTag() != null ? endpoint.getTag() : "";
                keyBuilder.append(topic).append("_").append(tag);
            }
            
            // 使用 SHA-256 生成稳定的哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyBuilder.toString().getBytes(StandardCharsets.UTF_8));
            
            // 将哈希值转换为十六进制字符串，取前32位（16字节）作为节点ID，确保唯一性
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 16 && i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String nodeId = hexString.toString();
            LogUtils.debug(String.format("生成节点ID: email=%s, nodeType=%s, nodeId=%s", email, nodeType, nodeId));
            return nodeId;
        } catch (Exception e) {
            LogUtils.error("生成节点ID失败，使用IDGenerator: " + e.getMessage(), e);
            // 如果生成失败，使用 IDGenerator 作为备选方案
            return IDGenerator.nextStr();
        }
    }

    /**
     * 更新节点数据
     * @param nodeId 节点ID
     * @param email 用户邮箱（用于验证权限）
     * @param endpointData 新的节点数据
     * @return 更新后的节点DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public PluginSyncNodeDTO updateNode(String nodeId, String email, Map<String, Object> endpointData) {
        WorkflowPluginSyncNode node = pluginSyncNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new RuntimeException("节点不存在: " + nodeId);
        }
        
        if (!email.equals(node.getEmail())) {
            throw new RuntimeException("无权更新该节点，节点不属于当前用户");
        }
        
        if (node.getDeletedTime() != null) {
            throw new RuntimeException("节点已被删除，无法更新");
        }
        
        long currentTime = System.currentTimeMillis();
        node.setEndpointData(endpointData);
        node.setUpdateTime(currentTime);
        
        int updateResult = pluginSyncNodeMapper.updateById(node);
        if (updateResult <= 0) {
            throw new RuntimeException("更新节点失败");
        }
        
        LogUtils.info("成功更新节点: email={}, nodeId={}", email, nodeId);
        return convertToDTO(node);
    }

    /**
     * 转换为 DTO
     */
    private PluginSyncNodeDTO convertToDTO(WorkflowPluginSyncNode node) {
        PluginSyncNodeDTO dto = new PluginSyncNodeDTO();
        dto.setNodeId(node.getNodeId());
        dto.setEmail(node.getEmail());
        dto.setNodeType(node.getNodeType());
        dto.setEndpointData(node.getEndpointData());
        dto.setCreateTime(node.getCreateTime());
        dto.setUpdateTime(node.getUpdateTime());
        return dto;
    }
}

