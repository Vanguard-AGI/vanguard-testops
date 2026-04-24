package io.vanguard.testops.bug.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.bug.domain.BugContent;
import io.vanguard.testops.bug.domain.BugExample;
import io.vanguard.testops.bug.enums.BugPlatform;
import io.vanguard.testops.bug.mapper.BugContentMapper;
import io.vanguard.testops.bug.mapper.BugMapper;
import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.uid.NumGenerator;
import io.vanguard.testops.sdk.constants.ApplicationNumScope;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 一次性迁移：将飞书项目中所有缺陷拉取到本地 Bug 表。
 * 通过缺陷关联的需求(Story) → TestPlan.feishuStoryId → projectId 自动路由到正确项目。
 */
@Service
public class FeishuDefectMigrationService {

    @Resource
    private FeishuMeegoService feishuMeegoService;
    @Resource
    private BugMapper bugMapper;
    @Resource
    private BugContentMapper bugContentMapper;
    @Resource
    private FeishuWebhookService feishuWebhookService;

    public int migrateAll(String projectKey, String userEmail) {
        List<JsonNode> items = feishuMeegoService.fetchAllDefects(projectKey, userEmail);
        int created = 0;
        int skipped = 0;

        for (JsonNode item : items) {
            String workItemId = item.path("id").asText(item.path("work_item_id").asText(""));
            if (StringUtils.isBlank(workItemId)) continue;

            if (existsByPlatformBugId(workItemId)) {
                skipped++;
                continue;
            }

            // 从飞书缺陷数据中提取关联的 Story ID
            String linkedStoryId = extractLinkedStoryId(item);
            String projectId = feishuWebhookService.resolveProjectIdByStoryId(linkedStoryId);

            String name = item.path("name").asText("飞书缺陷-" + workItemId);
            String description = extractDescription(item);
            String status = extractFieldValue(item, "status");
            String handleUser = extractFieldValue(item, "assignee");

            Bug bug = new Bug();
            bug.setId(IDGenerator.nextStr());
            bug.setTitle(name);
            bug.setProjectId(StringUtils.isNotBlank(projectId) ? projectId : "");
            bug.setTemplateId("");
            bug.setPlatform(BugPlatform.FEISHU.getName());
            bug.setPlatformBugId(workItemId);
            bug.setStatus(StringUtils.isNotBlank(status) ? status : "new");
            bug.setHandleUser(StringUtils.isNotBlank(handleUser) ? handleUser : "");
            bug.setCreateUser("migration");
            bug.setCreateTime(System.currentTimeMillis());
            bug.setUpdateTime(System.currentTimeMillis());
            // 迁移缺陷也生成业务 ID，避免 num 为 0
            bug.setNum(Long.valueOf(NumGenerator.nextNum(projectId, ApplicationNumScope.BUG_MANAGEMENT)).intValue());
            bug.setDeleted(false);
            bug.setPos(0L);
            bugMapper.insertSelective(bug);

            if (StringUtils.isNotBlank(description)) {
                BugContent content = new BugContent();
                content.setBugId(bug.getId());
                content.setDescription(description);
                bugContentMapper.insertSelective(content);
            }

            created++;
            LogUtils.info("[Migration] 迁移缺陷: workItemId={} -> bugId={}, projectId={}, storyId={}",
                    workItemId, bug.getId(), projectId, linkedStoryId);
        }
        LogUtils.info("[Migration] 迁移完成: 共 {} 条, 新增 {} 条, 跳过(已存在) {} 条", items.size(), created, skipped);
        return created;
    }

    private boolean existsByPlatformBugId(String platformBugId) {
        BugExample example = new BugExample();
        example.createCriteria()
                .andPlatformEqualTo(BugPlatform.FEISHU.getName())
                .andPlatformBugIdEqualTo(platformBugId);
        return bugMapper.countByExample(example) > 0;
    }

    /**
     * 从飞书工作项中提取关联的 Story ID。
     * 飞书工作项的 connections 字段存储关联关系。
     */
    private String extractLinkedStoryId(JsonNode item) {
        JsonNode connections = item.path("connections");
        if (connections.isArray()) {
            for (JsonNode conn : connections) {
                String typeKey = conn.path("work_item_type_key").asText("");
                if ("story".equalsIgnoreCase(typeKey)) {
                    return conn.path("work_item_id").asText("");
                }
            }
        }
        // 也检查 field_value_pairs 中是否有关联需求字段
        JsonNode fields = item.path("field_value_pairs");
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                String fieldKey = field.path("field_key").asText("");
                if (fieldKey.contains("story") || fieldKey.contains("connection")) {
                    return field.path("field_value").asText("");
                }
            }
        }
        return "";
    }

    private String extractDescription(JsonNode item) {
        JsonNode fields = item.path("fields");
        if (fields.isMissingNode()) {
            fields = item.path("field_value_pairs");
        }
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                if ("description".equals(field.path("field_key").asText())) {
                    return field.path("field_value").asText("");
                }
            }
        }
        return "";
    }

    private String extractFieldValue(JsonNode item, String fieldKey) {
        JsonNode fields = item.path("fields");
        if (fields.isMissingNode()) {
            fields = item.path("field_value_pairs");
        }
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                if (fieldKey.equals(field.path("field_key").asText())) {
                    return field.path("field_value").asText("");
                }
            }
        }
        return "";
    }
}
