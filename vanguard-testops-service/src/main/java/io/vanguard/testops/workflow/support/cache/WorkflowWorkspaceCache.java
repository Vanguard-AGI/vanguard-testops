package io.vanguard.testops.workflow.support.cache;

import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.workflow.dto.WorkflowWorkspaceDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工作空间列表缓存工具类
 * 用于缓存空间列表查询结果，提升性能
 */
@Component
public class WorkflowWorkspaceCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存键前缀
     */
    private static final String CACHE_PREFIX = "workspace:list:";

    /**
     * 缓存过期时间（分钟）
     */
    private static final long CACHE_EXPIRE_MINUTES = 5;

    /**
     * 获取缓存键
     */
    private String getCacheKey(String projectId, String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            return CACHE_PREFIX + projectId + ":" + keyword;
        }
        return CACHE_PREFIX + projectId;
    }

    /**
     * 获取空间列表缓存
     */
    public List<WorkflowWorkspaceDTO> get(String projectId, String keyword) {
        try {
            String cacheKey = getCacheKey(projectId, keyword);
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.isNotBlank(cacheValue)) {
                return JSON.parseArray(cacheValue, WorkflowWorkspaceDTO.class);
            }
        } catch (Exception e) {
            // 缓存读取失败不影响业务，记录日志即可
            // 这里不记录日志，避免日志过多
        }
        return null;
    }

    /**
     * 设置空间列表缓存
     */
    public void set(String projectId, String keyword, List<WorkflowWorkspaceDTO> workspaceList) {
        try {
            if (workspaceList == null) {
                return;
            }
            String cacheKey = getCacheKey(projectId, keyword);
            String cacheValue = JSON.toJSONString(workspaceList);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    cacheValue,
                    CACHE_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            // 缓存写入失败不影响业务，记录日志即可
            // 这里不记录日志，避免日志过多
        }
    }

    /**
     * 清除指定项目的所有空间列表缓存（包括带关键词的）
     * 当空间创建、更新、删除时调用
     */
    public void evictByProjectId(String projectId) {
        try {
            // 使用模式匹配删除所有相关缓存
            String pattern = CACHE_PREFIX + projectId + "*";
            stringRedisTemplate.delete(stringRedisTemplate.keys(pattern));
        } catch (Exception e) {
            // 缓存删除失败不影响业务
        }
    }

    /**
     * 清除指定空间的缓存
     * 当空间更新时调用
     */
    public void evictByWorkspaceId(String workspaceId, String projectId) {
        // 由于缓存是按 projectId 和 keyword 存储的，无法直接通过 workspaceId 删除
        // 所以直接清除整个项目的缓存
        evictByProjectId(projectId);
    }
}
