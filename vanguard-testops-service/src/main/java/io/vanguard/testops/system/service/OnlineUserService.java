package io.vanguard.testops.system.service;

import io.vanguard.testops.system.dto.OnlineUserDetail;
import io.vanguard.testops.system.dto.OnlineUserStats;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线用户：基于 Redis Session 扫描，按 sessionAttr:user 解析 userId，统计在线用户数、明细、在线时长
 */
@Service
public class OnlineUserService {

    /** Session 最大保留天数，在线时长不超过此值 */
    private static final int SESSION_MAX_DAYS = 7;
    private static final long SESSION_MAX_SECONDS = (long) SESSION_MAX_DAYS * 24 * 60 * 60;

    @Resource
    @Lazy
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    @Lazy
    private RedisIndexedSessionRepository redisIndexedSessionRepository;
    @Resource
    private BaseUserMapper baseUserMapper;

    /**
     * 获取在线用户统计：数量 + 明细（含会话数、在线时长）
     * 复用 SessionConfig.cleanSession 的 Redis Session 扫描与 sessionAttr:user 解析逻辑
     */
    public OnlineUserStats getOnlineUserStats() {
        // userId -> { sessionCount, totalDurationSeconds }
        Map<String, SessionAggregate> aggregateByUser = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match("spring:session:sessions:*").count(1000).build();
        long nowMs = System.currentTimeMillis();
        try (Cursor<String> scan = stringRedisTemplate.scan(options)) {
            while (scan.hasNext()) {
                String key = scan.next();
                if (StringUtils.contains(key, "spring:session:sessions:expires:")) {
                    continue;
                }
                Boolean hasUser = stringRedisTemplate.opsForHash().hasKey(key, "sessionAttr:user");
                if (!Boolean.TRUE.equals(hasUser)) {
                    continue;
                }
                // 只统计未过期的 session：session 过期(ttl=0)或已删除(ttl=-2) 视为不在线，不纳入统计；ttl>0 未过期，ttl=-1 未设置过期
                Long ttl = redisIndexedSessionRepository.getSessionRedisOperations().getExpire(key);
                if (ttl != null && (ttl == 0 || ttl == -2)) {
                    continue;
                }
                Object userObj = redisIndexedSessionRepository.getSessionRedisOperations().opsForHash().get(key, "sessionAttr:user");
                if (userObj == null) {
                    continue;
                }
                String userId;
                try {
                    userId = (String) MethodUtils.invokeMethod(userObj, "getId");
                } catch (Exception e) {
                    continue;
                }
                if (StringUtils.isBlank(userId)) {
                    continue;
                }
                Long creationMs = null;
                Object creationTimeObj = redisIndexedSessionRepository.getSessionRedisOperations().opsForHash().get(key, "creationTime");
                if (creationTimeObj instanceof Number) {
                    creationMs = ((Number) creationTimeObj).longValue();
                }
                aggregateByUser.computeIfAbsent(userId, k -> new SessionAggregate()).add(1, creationMs);
            }
        } catch (Exception e) {
            // ignore scan errors
        }
        List<String> userIds = new ArrayList<>(aggregateByUser.keySet());
        Map<String, UserDTO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (String id : userIds) {
                UserDTO dto = baseUserMapper.selectById(id);
                if (dto != null) {
                    userMap.put(id, dto);
                }
            }
        }
        List<OnlineUserDetail> details = aggregateByUser.entrySet().stream()
                .map(e -> {
                    OnlineUserDetail d = new OnlineUserDetail();
                    d.setUserId(e.getKey());
                    SessionAggregate agg = e.getValue();
                    d.setSessionCount(agg.sessionCount);
                    d.setCreationTime(agg.latestCreationTime);
                    long rawSeconds = agg.latestCreationTime != null ? Math.max(0, (nowMs - agg.latestCreationTime) / 1000) : 0L;
                    d.setOnlineDurationSeconds(Math.min(rawSeconds, SESSION_MAX_SECONDS));
                    UserDTO u = userMap.get(e.getKey());
                    if (u != null) {
                        d.setName(u.getName());
                        d.setEmail(u.getEmail());
                    }
                    return d;
                })
                .sorted(Comparator.comparing(OnlineUserDetail::getUserId))
                .collect(Collectors.toList());
        OnlineUserStats stats = new OnlineUserStats();
        stats.setCount(details.size());
        stats.setDetails(details);
        return stats;
    }

    private static class SessionAggregate {
        int sessionCount = 0;
        /** 最近一次登录的会话创建时间（取最大值），用于 creationTime 与 onlineDurationSeconds 计算 */
        Long latestCreationTime = null;

        void add(int sessions, Long creationMs) {
            this.sessionCount += sessions;
            if (creationMs != null) {
                this.latestCreationTime = this.latestCreationTime == null ? creationMs : Math.max(this.latestCreationTime, creationMs);
            }
        }
    }
}
