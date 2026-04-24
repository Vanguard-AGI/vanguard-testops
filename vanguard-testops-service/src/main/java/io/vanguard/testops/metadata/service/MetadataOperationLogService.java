package io.vanguard.testops.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vanguard.testops.metadata.domain.MetadataOperationLog;
import io.vanguard.testops.metadata.dto.AnalyticsTrackRequest;
import io.vanguard.testops.metadata.dto.MetadataOperationLogDTO;
import io.vanguard.testops.metadata.mapper.MetadataOperationLogMapper;
import io.vanguard.testops.project.service.ProjectUserRoleService;
import io.vanguard.testops.sdk.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MetadataOperationLogService {

    @Autowired
    private MetadataOperationLogMapper metadataOperationLogMapper;

    @Autowired
    private ProjectUserRoleService projectUserRoleService;

    @Transactional(rollbackFor = Exception.class)
    public void trackEvents(List<AnalyticsTrackRequest.Event> events) {
        if (events == null || events.isEmpty()) {
            LogUtils.warn("埋点事件列表为空，跳过处理");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (AnalyticsTrackRequest.Event event : events) {
            try {
                MetadataOperationLog log = convertToLog(event);
                if (log != null) {
                    metadataOperationLogMapper.insert(log);
                    successCount++;
                } else {
                    failCount++;
                    LogUtils.warn("事件转换失败，跳过: event={}", event.getEvent());
                }
            } catch (Exception e) {
                failCount++;
                LogUtils.error("保存埋点日志失败: event=" + event.getEvent() + ", error=" + e.getMessage(), e);
            }
        }

        LogUtils.info("埋点日志处理完成: 成功={}, 失败={}", successCount, failCount);
    }

    private MetadataOperationLog convertToLog(AnalyticsTrackRequest.Event event) {
        if (event == null || StringUtils.isBlank(event.getEvent()) || StringUtils.isBlank(event.getEmail())) {
            return null;
        }

        MetadataOperationLog log = new MetadataOperationLog();
        
        log.setLogId(UUID.randomUUID().toString());
        
        Integer bizType = parseBizType(event.getPlatform());
        if (bizType == null) {
            LogUtils.warn("无法解析业务类型: platform={}", event.getPlatform());
            return null;
        }
        log.setBizType(bizType);
        
        String moduleType = parseModuleType(event.getPage(), event.getProperties());
        if (StringUtils.isBlank(moduleType)) {
            moduleType = "HTTP";
        }
        log.setModuleType(moduleType);

        // AegisGO 特殊逻辑：仅根据 platform 判断，Plugin/AegisGo/Electron 均按 AegisGO 处理（projectId/relatedId 可为空）
        boolean isAegisGO = isAegisGOPlatform(event.getPlatform());

        String projectId = extractProjectId(event.getProperties());
        if (StringUtils.isBlank(projectId) && StringUtils.isNotBlank(event.getEmail())) {
            // 无项目 ID 时按用户邮箱查其最早项目（排除示例项目 100001100001）用于落库
            try {
                projectId = projectUserRoleService.getEarliestProjectIdByEmail(event.getEmail());
            } catch (Exception e) {
                LogUtils.warn("按邮箱解析最早项目失败: email={}, error={}", event.getEmail(), e.getMessage());
            }
        }
        if (StringUtils.isBlank(projectId)) {
            if (isAegisGO) {
                log.setProjectId(null);
            } else {
                LogUtils.warn("无法提取项目ID，跳过该事件");
                return null;
            }
        } else {
            log.setProjectId(projectId);
        }
        
        String relatedId = extractRelatedId(event.getProperties(), event.getPlatform());
        log.setRelatedId(relatedId);
        
        log.setAction(event.getEvent());
        log.setUserEmail(event.getEmail());
        
        Long executionTimeMs = extractExecutionTime(event.getProperties(), event);
        log.setExecutionTimeMs(executionTimeMs);
        
        log.setExtraData(event.getProperties());
        
        if (event.getTimestamp() != null) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getTimestamp()),
                ZoneId.systemDefault()
            );
            log.setCreatedAt(createdAt);
            log.setUpdatedAt(createdAt);
        } else {
            LocalDateTime now = LocalDateTime.now();
            log.setCreatedAt(now);
            log.setUpdatedAt(now);
        }
        
        return log;
    }

    /**
     * 仅根据 platform 判断是否为 AegisGO 系（Plugin/AegisGo/Electron），用于 projectId/relatedId 可空等特殊逻辑。
     */
    private boolean isAegisGOPlatform(String platform) {
        if (StringUtils.isBlank(platform)) {
            return false;
        }
        String p = platform.toUpperCase().trim();
        return p.contains("PLUGIN") || p.contains("AEGISGO") || p.contains("AEGIS-GO") || p.contains("ELECTRON");
    }

    /**
     * 根据 platform 解析落库 bizType：
     * 1 = AegisOne / Web；2 = Plugin / AegisGo；3 = Electron。
     * 2 与 3 在业务上均按 AegisGO 特殊逻辑处理，仅落库时区分。
     */
    private Integer parseBizType(String platform) {
        if (StringUtils.isBlank(platform)) {
            return 1;
        }
        String platformUpper = platform.toUpperCase().trim();
        if (platformUpper.contains("AEGISONE") || platformUpper.contains("AEGIS-ONE")) {
            return 1;
        }
        if (platformUpper.contains("ELECTRON")) {
            return 3;
        }
        // Plugin、AegisGo(AEGISGO / AEGIS-GO) → bizType 2
        if (platformUpper.contains("PLUGIN") || platformUpper.contains("AEGISGO") || platformUpper.contains("AEGIS-GO")) {
            return 2;
        }
        return 1;
    }

    private String parseModuleType(String page, Map<String, Object> properties) {
        if (StringUtils.isNotBlank(page)) {
            return page.toUpperCase();
        }
        
        if (properties != null) {
            if (properties.containsKey("interfaceName") || properties.containsKey("methodName")) {
                return "DUBBO";
            }
            if (properties.containsKey("sql")) {
                return "SQL";
            }
            if (properties.containsKey("topic") || properties.containsKey("messageKey")) {
                return "ROCKETMQ";
            }
        }
        
        return "HTTP";
    }

    private String extractProjectId(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        
        if (properties.containsKey("projectId")) {
            Object projectId = properties.get("projectId");
            return projectId != null ? projectId.toString() : null;
        }
        
        return null;
    }

    private String extractRelatedId(Map<String, Object> properties, String platform) {
        if (properties == null) {
            return null;
        }

        // AegisGO 系（Plugin/AegisGo/Electron）：definitionId 可为空
        boolean isAegisGO = isAegisGOPlatform(platform);

        if (properties.containsKey("definitionId")) {
            Object definitionId = properties.get("definitionId");
            return definitionId != null ? definitionId.toString() : null;
        }
        
        if (isAegisGO) {
            return null;
        }
        
        return null;
    }

    private Long extractExecutionTime(Map<String, Object> properties, AnalyticsTrackRequest.Event event) {
        if (event != null && event.getDuration() != null) {
            return event.getDuration();
        }
        
        if (properties != null && properties.containsKey("duration")) {
            Object duration = properties.get("duration");
            if (duration instanceof Number) {
                return ((Number) duration).longValue();
            }
        }
        
        if (properties != null && properties.containsKey("executionTimeMs")) {
            Object executionTime = properties.get("executionTimeMs");
            if (executionTime instanceof Number) {
                return ((Number) executionTime).longValue();
            }
        }
        
        return null;
    }

    public List<MetadataOperationLogDTO> getRecentLogsByUser(String userEmail, String projectId, int limit) {
        if (StringUtils.isBlank(userEmail)) {
            return List.of();
        }

        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MetadataOperationLog::getUserEmail, userEmail)
                .eq(MetadataOperationLog::getBizType, 1);
        
        if (StringUtils.isNotBlank(projectId)) {
            queryWrapper.eq(MetadataOperationLog::getProjectId, projectId);
        }
        
        queryWrapper.orderByDesc(MetadataOperationLog::getCreatedAt)
                .last("LIMIT " + limit);

        List<MetadataOperationLog> logs = metadataOperationLogMapper.selectList(queryWrapper);
        return logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MetadataOperationLogDTO convertToDTO(MetadataOperationLog log) {
        if (log == null) {
            return null;
        }

        MetadataOperationLogDTO dto = new MetadataOperationLogDTO();
        dto.setId(log.getId());
        dto.setLogId(log.getLogId());
        dto.setBizType(log.getBizType());
        dto.setModuleType(log.getModuleType());
        dto.setProjectId(log.getProjectId());
        dto.setRelatedId(log.getRelatedId());
        dto.setAction(log.getAction());
        dto.setUserEmail(log.getUserEmail());
        dto.setExecutionTimeMs(log.getExecutionTimeMs());
        dto.setExtraData(log.getExtraData());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setUpdatedAt(log.getUpdatedAt());
        return dto;
    }
}

