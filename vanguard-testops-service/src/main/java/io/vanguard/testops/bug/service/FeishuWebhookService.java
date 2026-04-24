package io.vanguard.testops.bug.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanguard.testops.bug.constants.FeishuOptionMapping;
import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.bug.domain.BugContent;
import io.vanguard.testops.bug.domain.BugCustomField;
import io.vanguard.testops.bug.domain.BugExample;
import io.vanguard.testops.bug.enums.BugPlatform;
import io.vanguard.testops.bug.mapper.BugContentMapper;
import io.vanguard.testops.bug.mapper.BugCustomFieldMapper;
import io.vanguard.testops.bug.mapper.BugMapper;
import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.plan.domain.TestPlan;
import io.vanguard.testops.project.dto.ProjectTemplateOptionDTO;
import io.vanguard.testops.project.service.ProjectTemplateService;
import io.vanguard.testops.sdk.constants.ApplicationNumScope;
import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.plan.domain.TestPlanExample;
import io.vanguard.testops.plan.mapper.TestPlanMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.dto.user.UserExcludeOptionDTO;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.uid.NumGenerator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 飞书 Webhook 事件分发与处理。
 * 通过缺陷关联的需求(Story) → TestPlan.feishuStoryId → projectId 自动路由到正确的项目。
 */
@Service
public class FeishuWebhookService {

    @Resource
    private BugMapper bugMapper;
    @Resource
    private BugContentMapper bugContentMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private FeishuMeegoService feishuMeegoService;
    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private ProjectTemplateService projectTemplateService;
    @Resource
    private BaseUserMapper baseUserMapper;

    @Resource
    private BugCustomFieldMapper bugCustomFieldMapper;

    /** 飞书 user_key → 本系统用户 ID 映射，格式：userKey1:userId1,userKey2:userId2，可选 */
    @Value("${feishu.meego.user-key-to-system-user-id:}")
    private String userKeyToSystemUserIdMapping;
    /** 缺陷模板中「产品经理」自定义字段的 field_id，配置后飞书 role_b9fbfb 会写入该自定义字段 */
    @Value("${feishu.meego.product-manager-field-id:}")
    private String productManagerFieldId;
    /** 缺陷模板中「优先级」自定义字段的 field_id，配置后飞书 priority 会写入该自定义字段（列表「优先级」列即读此字段） */
    @Value("${feishu.meego.priority-field-id:}")
    private String priorityFieldId;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public void dispatch(Map<String, Object> payload) {
        if (payload == null) {
            LogUtils.warn("[FeishuWebhook] payload 为空");
            return;
        }
        // 兼容飞书新版：事件数据可能在 event 或 payload 字段
        Map<String, Object> event = (Map<String, Object>) payload.getOrDefault("event", payload.getOrDefault("payload", Collections.emptyMap()));
        String eventId = extractString(payload, "header", "event_id");
        if (StringUtils.isBlank(eventId)) {
            eventId = extractString(payload, "header", "uuid");
        }
        LogUtils.info("[FeishuWebhook] dispatch: eventId={}, header.event_type={}, event.keys={}",
                eventId, extractString(payload, "header", "event_type"), event.keySet());

        if (StringUtils.isNotBlank(eventId) && !processedEventIds.add(eventId)) {
            LogUtils.info("[FeishuWebhook] 重复事件跳过: eventId={}", eventId);
            return;
        }

        String eventType = extractString(payload, "header", "event_type");
        if (StringUtils.isBlank(eventType)) {
            LogUtils.warn("[FeishuWebhook] 无法识别 event_type, body.header={}", payload.get("header"));
            return;
        }

        // 飞书新版 event_type 可能是 WorkitemCreateEvent / WorkitemUpdateEvent / WorkitemDeleteEvent
        String eventTypeNorm = normalizeEventType(eventType);
        String workItemTypeKey = String.valueOf(event.getOrDefault("work_item_type_key", ""));
        String workItemId = String.valueOf(event.getOrDefault("work_item_id", event.getOrDefault("id", "")));

        LogUtils.info("[FeishuWebhook] 解析: eventType={}, eventTypeNorm={}, workItemTypeKey={}, workItemId={}, defectTypeKey={}",
                eventType, eventTypeNorm, workItemTypeKey, workItemId, feishuMeegoService.getDefectTypeKey());

        if (!StringUtils.equals(workItemTypeKey, feishuMeegoService.getDefectTypeKey())
                && !isDefectTypeKey(workItemTypeKey)) {
            LogUtils.info("[FeishuWebhook] 非缺陷类型事件，跳过: workItemTypeKey={}", workItemTypeKey);
            return;
        }

        String projectKey = String.valueOf(event.getOrDefault("project_key", ""));
        switch (eventTypeNorm) {
            case "work_item.created", "work_item.all_fields_updated" -> handleCreateOrUpdate(workItemId, projectKey, event);
            case "work_item.deleted" -> handleDelete(workItemId);
            default -> LogUtils.info("[FeishuWebhook] 未处理的事件类型: {}", eventType);
        }
    }

    /** 飞书新版 event_type 转为统一小写+下划线格式 */
    private String normalizeEventType(String eventType) {
        if (eventType == null) return "";
        if (eventType.startsWith("work_item.")) return eventType;
        return switch (eventType) {
            case "WorkitemCreateEvent" -> "work_item.created";
            case "WorkitemUpdateEvent", "WorkitemAllFieldsUpdatedEvent", "WorkitemStatusEvent" -> "work_item.all_fields_updated";
            case "WorkitemDeleteEvent" -> "work_item.deleted";
            default -> eventType;
        };
    }

    private boolean isDefectTypeKey(String workItemTypeKey) {
        return "defect".equalsIgnoreCase(workItemTypeKey)
                || StringUtils.equals(workItemTypeKey, feishuMeegoService.getDefectTypeKey());
    }

    /**
     * Webhook 只当触发器，收到事件后调飞书「工作项详情」API 拿全量字段，再从稳定的 API 响应结构解析落库。
     * 不再逐字段解析 Webhook 事件体（结构不稳定、字段不全）。
     */
    @SuppressWarnings("unchecked")
    private void handleCreateOrUpdate(String workItemId, String projectKey, Map<String, Object> event) {
        if (StringUtils.isBlank(workItemId)) {
            LogUtils.warn("[FeishuWebhook] handleCreateOrUpdate 跳过: workItemId 为空");
            return;
        }

        // ========== 1. 调飞书详情 API 拿全量数据 ==========
        JsonNode detail = fetchWorkItemDetail(projectKey, workItemId);
        if (detail == null) {
            LogUtils.warn("[FeishuWebhook] 调飞书详情 API 失败，回退到解析事件体: workItemId={}", workItemId);
            handleCreateOrUpdateFromEvent(workItemId, event);
            return;
        }

        // ========== 2. 从 API 响应的稳定结构解析所有字段 ==========
        String name = detail.path("name").asText("");
        String description = parseDescriptionFromDetail(detail);
        String status = parseStatusFromDetail(detail, projectKey);
        String linkedStoryId = parseStoryIdFromDefectDetail(detail);
        String priority = parsePriorityFromDetail(detail);
        String createdBy = detail.path("created_by").asText("");
        String updatedBy = detail.path("updated_by").asText("");
        // 经办人：飞书对接标识为 owner（负责人），先按 owner 取，再回退 role_owners.operator
        String handleUser = parseUserKeyFromDetail(detail, "owner");
        if (StringUtils.isBlank(handleUser)) {
            handleUser = parseRoleOwnerFromDetail(detail, "operator");
        }
        String productManager = parseRoleOwnerFromDetail(detail, "role_b9fbfb");
        String reporter = parseRoleOwnerFromDetail(detail, "reporter");
        String defectTypeRaw = parseSimpleFieldFromDetail(detail, "template");
        String defectType = mapFeishuDefectType(defectTypeRaw);
        String defectReason = parseTreeSelectPathFromDetail(detail, "field_e84b00", "bug_reason");
        String appId = parseSimpleFieldFromDetail(detail, "field_39dbe4");
        String discoveryPhaseRaw = parseSimpleFieldFromDetail(detail, "field_1cbc4e");
        String discoveryPhase = FeishuOptionMapping.toDiscoveryPhaseDisplay(discoveryPhaseRaw);
        String discoveryDifficultyRaw = parseSimpleFieldFromDetail(detail, "field_6b822e");
        String discoveryDifficulty = FeishuOptionMapping.toDiscoveryDifficultyDisplay(discoveryDifficultyRaw);
        String discovererRaw = parseTreeSelectLeafLabelFromDetail(detail, "field_f12022");
        String discoverer = mapFeishuDiscoverer(discovererRaw);
        String businessLine = parseBusinessLineIdFromDetail(detail, projectKey);
        String affectedAppIds = parseSimpleFieldFromDetail(detail, "field_0b1b4f");
        Long actualTimeMs = parseDateFieldFromDetail(detail, "field_eb5b78");
        List<String> watcherUserKeys = parseMultiUserFromDetail(detail, "watchers");

        String handleUserMapped = resolveSystemUserIdFromFeishuUserKey(handleUser);
        String createUserMapped = resolveSystemUserIdFromFeishuUserKey(createdBy);
        String updateUserMapped = resolveSystemUserIdFromFeishuUserKey(updatedBy);
        String productManagerMapped = resolveSystemUserIdFromFeishuUserKey(productManager);
        String reporterMapped = resolveSystemUserIdFromFeishuUserKey(reporter);
        String priorityMapped = mapFeishuPriorityToSystem(priority);

        // ========== 3. 落库 ==========
        Bug existing = findByPlatformBugId(workItemId);
        if (existing == null && StringUtils.isNotBlank(linkedStoryId)) {
            String projectIdForMatch = resolveProjectIdByStoryId(linkedStoryId);
            existing = findRecentlyCreatedFeishuBugWithoutPlatformBugId(projectIdForMatch, 90_000);
            if (existing != null) {
                LogUtils.info("[FeishuWebhook] 匹配到本方刚创建未回写: bugId={}, workItemId={}", existing.getId(), workItemId);
            }
        }
        if (existing != null) {
            Bug update = new Bug();
            update.setId(existing.getId());
            if (StringUtils.isBlank(existing.getPlatformBugId())) {
                update.setPlatformBugId(workItemId);
            }
            if (StringUtils.isNotBlank(name)) update.setTitle(name);
            if (StringUtils.isNotBlank(status)) update.setStatus(status);
            if (StringUtils.isNotBlank(linkedStoryId)) update.setFeishuStoryId(linkedStoryId);
            if (StringUtils.isNotBlank(handleUserMapped)) update.setHandleUser(handleUserMapped);
            if (StringUtils.isNotBlank(updateUserMapped)) update.setUpdateUser(updateUserMapped);
            if (StringUtils.isNotBlank(defectType)) update.setDefectType(defectType);
            if (StringUtils.isNotBlank(defectReason)) update.setDefectReason(defectReason);
            if (StringUtils.isNotBlank(appId)) update.setAppId(appId);
            if (StringUtils.isNotBlank(affectedAppIds)) update.setAffectedAppIds(affectedAppIds);
            if (StringUtils.isNotBlank(discoveryPhase)) update.setDiscoveryPhase(discoveryPhase);
            if (StringUtils.isNotBlank(discoveryDifficulty)) update.setDiscoveryDifficulty(discoveryDifficulty);
            if (StringUtils.isNotBlank(discoverer)) update.setDiscoverer(discoverer);
            if (actualTimeMs != null) update.setActualTime(actualTimeMs);
            if (StringUtils.isNotBlank(businessLine)) update.setBusinessLine(businessLine);
            update.setUpdateTime(System.currentTimeMillis());
            bugMapper.updateByPrimaryKeySelective(update);

            if (StringUtils.isNotBlank(description)) {
                BugContent content = new BugContent();
                content.setBugId(existing.getId());
                content.setDescription(description);
                bugContentMapper.updateByPrimaryKeySelective(content);
            }
            saveProductManagerCustomField(existing.getId(), productManagerMapped);
            savePriorityCustomField(existing.getId(), priorityMapped);
            saveReporterCustomField(existing.getId(), reporterMapped);
            saveFollowerCustomField(existing.getId(), watcherUserKeys);
        } else {
            String projectId = resolveProjectIdByStoryId(linkedStoryId);
            String templateId = resolveTemplateId(projectId);
            Bug bug = new Bug();
            bug.setId(IDGenerator.nextStr());
            bug.setTitle(StringUtils.isNotBlank(name) ? name : "飞书缺陷-" + workItemId);
            bug.setProjectId(StringUtils.isNotBlank(projectId) ? projectId : "");
            bug.setTemplateId(StringUtils.isNotBlank(templateId) ? templateId : "");
            bug.setPlatform(BugPlatform.FEISHU.getName());
            bug.setPlatformBugId(workItemId);
            bug.setStatus(StringUtils.isNotBlank(status) ? status : "待确认");
            bug.setFeishuStoryId(StringUtils.isNotBlank(linkedStoryId) ? linkedStoryId : "");
            bug.setHandleUser(StringUtils.isNotBlank(handleUserMapped) ? handleUserMapped : "");
            bug.setCreateUser(StringUtils.isNotBlank(createUserMapped) ? createUserMapped : "admin");
            bug.setUpdateUser(StringUtils.isNotBlank(updateUserMapped) ? updateUserMapped : "admin");
            if (StringUtils.isNotBlank(defectType)) bug.setDefectType(defectType);
            if (StringUtils.isNotBlank(defectReason)) bug.setDefectReason(defectReason);
            if (StringUtils.isNotBlank(appId)) bug.setAppId(appId);
            if (StringUtils.isNotBlank(affectedAppIds)) bug.setAffectedAppIds(affectedAppIds);
            if (StringUtils.isNotBlank(discoveryPhase)) bug.setDiscoveryPhase(discoveryPhase);
            if (StringUtils.isNotBlank(discoveryDifficulty)) bug.setDiscoveryDifficulty(discoveryDifficulty);
            if (StringUtils.isNotBlank(discoverer)) bug.setDiscoverer(discoverer);
            if (actualTimeMs != null) bug.setActualTime(actualTimeMs);
            if (StringUtils.isNotBlank(businessLine)) bug.setBusinessLine(businessLine);
            bug.setCreateTime(System.currentTimeMillis());
            bug.setUpdateTime(System.currentTimeMillis());
            // 无论是否解析出 projectId，都生成业务 ID，保证 num 不为 0
            String numPrefix = StringUtils.isNotBlank(projectId) ? projectId : BugPlatform.FEISHU.getName();
            bug.setNum(Long.valueOf(NumGenerator.nextNum(numPrefix, ApplicationNumScope.BUG_MANAGEMENT)).intValue());
            bug.setDeleted(false);
            bug.setPos(0L);
            bugMapper.insertSelective(bug);

                BugContent content = new BugContent();
                content.setBugId(bug.getId());
            content.setDescription(StringUtils.isNotBlank(description) ? description : "");
            bugContentMapper.insertSelective(content);

            saveProductManagerCustomField(bug.getId(), productManagerMapped);
            savePriorityCustomField(bug.getId(), priorityMapped);
            saveReporterCustomField(bug.getId(), reporterMapped);
            saveFollowerCustomField(bug.getId(), watcherUserKeys);
        }
    }

    /** API 调用失败时的兜底：用旧逻辑从 Webhook event 体解析（保留向后兼容） */
    @SuppressWarnings("unchecked")
    private void handleCreateOrUpdateFromEvent(String workItemId, Map<String, Object> event) {
        Bug existing = findByPlatformBugId(workItemId);
        String name = extractTitleFromEvent(event);
        String description = extractDescription(event);
        String status = extractStatusFromEvent(event);
        String linkedStoryId = extractLinkedStoryId(event);
        if (existing == null && StringUtils.isNotBlank(linkedStoryId)) {
            String projectIdForMatch = resolveProjectIdByStoryId(linkedStoryId);
            existing = findRecentlyCreatedFeishuBugWithoutPlatformBugId(projectIdForMatch, 90_000);
            if (existing != null) {
                LogUtils.info("[FeishuWebhook] 兜底逻辑-匹配到本方刚创建未回写: bugId={}, workItemId={}", existing.getId(), workItemId);
            }
        }
        String handleUserFromFeishu = extractHandleUserFromEvent(event);
        String handleUserMapped = resolveSystemUserIdFromFeishuUserKey(handleUserFromFeishu);
        String createdByFromFeishu = extractCreatedByFromEvent(event);
        String updatedByFromFeishu = extractUpdatedByFromEvent(event);
        String createUserMapped = resolveSystemUserIdFromFeishuUserKey(createdByFromFeishu);
        String updateUserMapped = resolveSystemUserIdFromFeishuUserKey(updatedByFromFeishu);
        String priority = mapFeishuPriorityToSystem(extractPriorityFromEvent(event));

        if (existing != null) {
            Bug update = new Bug();
            update.setId(existing.getId());
            if (StringUtils.isBlank(existing.getPlatformBugId())) {
                update.setPlatformBugId(workItemId);
            }
            if (StringUtils.isNotBlank(name)) update.setTitle(name);
            if (StringUtils.isNotBlank(status)) update.setStatus(status);
            if (StringUtils.isNotBlank(handleUserMapped)) update.setHandleUser(handleUserMapped);
            if (StringUtils.isNotBlank(updateUserMapped)) update.setUpdateUser(updateUserMapped);
            update.setUpdateTime(System.currentTimeMillis());
            bugMapper.updateByPrimaryKeySelective(update);
            if (description != null) {
                BugContent content = new BugContent();
                content.setBugId(existing.getId());
                content.setDescription(description);
                bugContentMapper.updateByPrimaryKeySelective(content);
            }
            savePriorityCustomField(existing.getId(), priority);
        } else {
            String projectId = resolveProjectIdByStoryId(linkedStoryId);
            String templateId = resolveTemplateId(projectId);
            Bug bug = new Bug();
            bug.setId(IDGenerator.nextStr());
            bug.setTitle(StringUtils.isNotBlank(name) ? name : "飞书缺陷-" + workItemId);
            bug.setProjectId(StringUtils.isNotBlank(projectId) ? projectId : "");
            bug.setTemplateId(StringUtils.isNotBlank(templateId) ? templateId : "");
            bug.setPlatform(BugPlatform.FEISHU.getName());
            bug.setPlatformBugId(workItemId);
            bug.setStatus(StringUtils.isNotBlank(status) ? status : "待确认");
            bug.setHandleUser(StringUtils.isNotBlank(handleUserMapped) ? handleUserMapped : "");
            bug.setCreateUser(StringUtils.isNotBlank(createUserMapped) ? createUserMapped : "admin");
            bug.setUpdateUser(StringUtils.isNotBlank(updateUserMapped) ? updateUserMapped : "admin");
            bug.setCreateTime(System.currentTimeMillis());
            bug.setUpdateTime(System.currentTimeMillis());
            // 无论是否解析出 projectId，都生成业务 ID，保证 num 不为 0
            String numPrefix = StringUtils.isNotBlank(projectId) ? projectId : BugPlatform.FEISHU.getName();
            bug.setNum(Long.valueOf(NumGenerator.nextNum(numPrefix, ApplicationNumScope.BUG_MANAGEMENT)).intValue());
            bug.setDeleted(false);
            bug.setPos(0L);
            bugMapper.insertSelective(bug);
            BugContent content = new BugContent();
            content.setBugId(bug.getId());
            content.setDescription(description != null ? description : "");
                bugContentMapper.insertSelective(content);
            savePriorityCustomField(bug.getId(), priority);
        }
    }

    // ======================== 飞书详情 API 字段解析方法 ========================

    /** 调飞书工作项详情 API，返回单个工作项 JsonNode；失败返回 null */
    private JsonNode fetchWorkItemDetail(String projectKey, String workItemId) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemId)) return null;
        long wid;
        try {
            wid = Long.parseLong(workItemId);
        } catch (NumberFormatException e) {
            LogUtils.warn("[FeishuWebhook] workItemId 非数字: {}", workItemId);
            return null;
        }
        try {
            List<JsonNode> list = feishuMeegoService.getDefectDetails(projectKey, null, Collections.singletonList(wid));
            if (list != null && !list.isEmpty()) return list.get(0);
            LogUtils.warn("[FeishuWebhook] 飞书详情 API 返回空: projectKey={}, workItemId={}", projectKey, workItemId);
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 调飞书详情 API 异常: projectKey=" + projectKey + ", workItemId=" + workItemId + ", err=" + e.getMessage());
        }
        return null;
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /** 从 API multi_texts 中取描述。飞书 field_key 可能是自动生成的 ID（如 field_5c572c），
     * 所以优先按 field_alias 匹配"描述"关键词，匹配不到则取第一个非空的富文本。 */
    private String parseDescriptionFromDetail(JsonNode detail) {
        JsonNode multiTexts = detail.path("multi_texts");
        if (!multiTexts.isArray() || multiTexts.isEmpty()) {
            return parseSimpleFieldFromDetail(detail, "description", "desc");
        }
        // 先按 field_key / field_alias 精确匹配
        for (JsonNode mt : multiTexts) {
            String fk = mt.path("field_key").asText("");
            if (fk.contains("description") || fk.contains("desc")) {
                String text = extractMultiTextContent(mt);
                if (StringUtils.isNotBlank(text)) return text;
            }
        }
        // 再从 fields 里找 field_type_key=multi_text 且 field_alias 含"描述"/"缺陷内容"的 field_key，去 multi_texts 匹配
        String descFieldKey = findFieldKeyByAlias(detail, "描述", "缺陷内容", "description", "desc");
        if (StringUtils.isNotBlank(descFieldKey)) {
            for (JsonNode mt : multiTexts) {
                if (descFieldKey.equals(mt.path("field_key").asText(""))) {
                    String text = extractMultiTextContent(mt);
                    if (StringUtils.isNotBlank(text)) return text;
                }
            }
        }
        // 兜底：取第一个非空的 multi_text
        for (JsonNode mt : multiTexts) {
            String text = extractMultiTextContent(mt);
            if (StringUtils.isNotBlank(text)) return text;
        }
        return "";
    }

    private String extractMultiTextContent(JsonNode mt) {
        JsonNode fv = mt.path("field_value");
        if (fv.path("is_empty").asBoolean(true)) return "";
        String docText = fv.path("doc_text").asText("");
        if (StringUtils.isNotBlank(docText)) return docText.trim();
        String docHtml = fv.path("doc_html").asText("");
        if (StringUtils.isNotBlank(docHtml)) return docHtml.trim();
        return "";
    }

    /** 在 fields 数组里，按 field_alias 关键词查找对应的 field_key */
    private String findFieldKeyByAlias(JsonNode detail, String... aliasKeywords) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        for (JsonNode f : fields) {
            String alias = f.path("field_alias").asText("");
            String fk = f.path("field_key").asText("");
            for (String keyword : aliasKeywords) {
                if (alias.contains(keyword) || fk.contains(keyword)) return fk;
            }
        }
        return "";
    }

    /** 从 API work_item_status 中取状态，优先通过流程模板将 state_key 还原为展示名，再映射为系统枚举 */
    private String parseStatusFromDetail(JsonNode detail, String projectKey) {
        String raw = "";
        JsonNode status = detail.path("work_item_status");
        if (!status.isMissingNode() && status.isObject()) {
            raw = status.path("state_key").asText("");
            if (StringUtils.isBlank(raw)) {
                raw = status.path("name").asText("");
            }
        }
        if (StringUtils.isBlank(raw)) {
            JsonNode fields = detail.path("fields");
            if (fields.isArray()) {
                for (JsonNode f : fields) {
                    String fk = f.path("field_key").asText("");
                    if ("work_item_status".equals(fk) || "status".equals(fk)) {
                        JsonNode fv = f.path("field_value");
                        if (fv.isObject()) {
                            String sk = fv.path("state_key").asText("");
                            if (StringUtils.isBlank(sk)) {
                                sk = fv.path("name").asText("");
                            }
                            if (StringUtils.isNotBlank(sk)) {
                                raw = sk;
                                break;
                            }
                        }
                        if (fv.isTextual() && StringUtils.isNotBlank(fv.asText(""))) {
                            raw = fv.asText("");
                            break;
                        }
                    }
                }
            }
        }
        if (StringUtils.isBlank(raw)) {
            return "待确认";
        }
        String display = raw;
        if (StringUtils.isNotBlank(projectKey)) {
            try {
                display = feishuMeegoService.getWorkflowStateDisplayName(projectKey, null, raw);
            } catch (Exception ignored) {
            }
        }
        String mapped = FeishuOptionMapping.toStatusDisplay(display);
        if (StringUtils.isNotBlank(mapped)) {
            return mapped;
        }
        return display;
    }

    /** 从 API fields 中取优先级选项值（如 option_2），待后续映射 */
    private String parsePriorityFromDetail(JsonNode detail) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            if (!"priority".equals(fk)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isTextual()) return fv.asText("");
            if (fv.isObject()) {
                String val = fv.path("value").asText("");
                if (StringUtils.isNotBlank(val)) return val;
                val = fv.path("label").asText("");
                if (StringUtils.isNotBlank(val)) return val;
            }
            if (fv.isArray() && fv.size() > 0) {
                JsonNode first = fv.get(0);
                if (first.isTextual()) return first.asText("");
                if (first.isObject()) {
                    String val = first.path("value").asText("");
                    if (StringUtils.isNotBlank(val)) return val;
                }
            }
        }
        return "";
    }

    /** 从 API fields 中按 field_key 取用户类型字段的 user_key（如 owner=负责人） */
    private String parseUserKeyFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            if (!keys.contains(fk)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isObject()) {
                String uk = fv.path("user_key").asText("");
                if (StringUtils.isBlank(uk)) uk = fv.path("id").asText("");
                if (StringUtils.isNotBlank(uk)) return uk.trim();
            }
            if (fv.isArray() && fv.size() > 0) {
                JsonNode first = fv.get(0);
                if (first.isObject()) {
                    String uk = first.path("user_key").asText("");
                    if (StringUtils.isBlank(uk)) uk = first.path("id").asText("");
                    if (StringUtils.isNotBlank(uk)) return uk.trim();
                } else if (first.isTextual()) {
                    return first.asText("").trim();
                }
            }
            if (fv.isTextual() && StringUtils.isNotBlank(fv.asText(""))) {
                return fv.asText("").trim();
            }
        }
        return "";
    }

    /** 从 API fields 中取角色对应的人员 userKey（如 operator=经办人, role_b9fbfb=产品经理） */
    private String parseRoleOwnerFromDetail(JsonNode detail, String role) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            if ("role_owners".equals(fk)) {
                JsonNode fv = f.path("field_value");
                if (fv.isArray()) {
                    for (JsonNode roleNode : fv) {
                        if (role.equals(roleNode.path("role").asText(""))) {
                            JsonNode owners = roleNode.path("owners");
                            if (owners.isArray() && owners.size() > 0) {
                                return owners.get(0).asText("");
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * 从飞书工作项详情里解析业务线，并统一为飞书 businessId 落库。
     * 使用写死映射 FeishuOptionMapping.toBusinessLineId。
     */
    private String parseBusinessLineIdFromDetail(JsonNode detail, String projectKey) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) {
            return "";
        }
        String raw = "";
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!"business".equals(fk) && !"business".equals(alias)) {
                continue;
            }
            JsonNode fv = f.path("field_value");
            if (fv.isArray() && fv.size() > 0) {
                fv = fv.get(0);
            }
            if (fv.isObject()) {
                String id = fv.path("id").asText("");
                if (StringUtils.isNotBlank(id)) {
                    raw = id.trim();
                    break;
                }
                id = fv.path("value").asText("");
                if (StringUtils.isNotBlank(id)) {
                    raw = id.trim();
                    break;
                }
                String label = fv.path("label").asText("");
                if (StringUtils.isNotBlank(label)) {
                    raw = label.trim();
                    break;
                }
            } else if (fv.isTextual()) {
                String s = fv.asText("");
                if (StringUtils.isNotBlank(s)) {
                    raw = s.trim();
                    break;
                }
            }
        }
        if (StringUtils.isBlank(raw)) return "";
        return FeishuOptionMapping.toBusinessLineId(raw);
    }

    /** 从 API fields 中按 field_key 或 field_alias 取简单值（文本、选项 label、tree_select label 等） */
    private String parseSimpleFieldFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isTextual()) return fv.asText("").trim();
            if (fv.isObject()) {
                String label = fv.path("label").asText("");
                if (StringUtils.isNotBlank(label)) return label.trim();
                String value = fv.path("value").asText("");
                if (StringUtils.isNotBlank(value)) return value.trim();
                // 飞书 template 等字段可能只返回 id（无 label/value）
                String id = fv.path("id").asText("");
                if (StringUtils.isNotBlank(id)) return id.trim();
            }
            if (fv.isArray() && fv.size() > 0) {
                JsonNode first = fv.get(0);
                if (first.isTextual()) return first.asText("").trim();
                if (first.isObject()) {
                    String label = first.path("label").asText("");
                    if (StringUtils.isNotBlank(label)) return label.trim();
                }
            }
        }
        return "";
    }

    /** 若配置了产品经理自定义字段 id，将飞书产品经理（role_b9fbfb）映射后的用户 ID 写入 bug_custom_field */
    private void saveProductManagerCustomField(String bugId, String productManagerSystemUserId) {
        if (StringUtils.isBlank(bugId) || StringUtils.isBlank(productManagerFieldId)) return;
        if (StringUtils.isBlank(productManagerSystemUserId)) return;
        try {
            BugCustomField existing = bugCustomFieldMapper.selectByPrimaryKey(bugId, productManagerFieldId);
            String value = productManagerSystemUserId;
            String content = value;
            if (existing != null) {
                BugCustomField update = new BugCustomField();
                update.setBugId(bugId);
                update.setFieldId(productManagerFieldId);
                update.setValue(value);
                update.setContent(content);
                bugCustomFieldMapper.updateByPrimaryKeySelective(update);
            } else {
                BugCustomField insert = new BugCustomField();
                insert.setBugId(bugId);
                insert.setFieldId(productManagerFieldId);
                insert.setValue(value);
                insert.setContent(content);
                bugCustomFieldMapper.insertSelective(insert);
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 写入产品经理自定义字段失败: bugId=" + bugId + ", fieldId=" + productManagerFieldId + ", e=" + e.getMessage());
        }
    }

    /** 报告人/发现人写入 bug_custom_field（id='reporter'），平台编辑表单从这里读取 */
    private void saveReporterCustomField(String bugId, String reporterSystemUserId) {
        if (StringUtils.isBlank(bugId) || StringUtils.isBlank(reporterSystemUserId)) return;
        String fieldId = "reporter";
        try {
            BugCustomField existing = bugCustomFieldMapper.selectByPrimaryKey(bugId, fieldId);
            if (existing != null) {
                BugCustomField update = new BugCustomField();
                update.setBugId(bugId);
                update.setFieldId(fieldId);
                update.setValue(reporterSystemUserId);
                update.setContent(reporterSystemUserId);
                bugCustomFieldMapper.updateByPrimaryKeySelective(update);
            } else {
                BugCustomField insert = new BugCustomField();
                insert.setBugId(bugId);
                insert.setFieldId(fieldId);
                insert.setValue(reporterSystemUserId);
                insert.setContent(reporterSystemUserId);
                bugCustomFieldMapper.insertSelective(insert);
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 写入报告人自定义字段失败: bugId=" + bugId + ", e=" + e.getMessage());
        }
    }

    /** 从飞书 watchers (multi_user) 解析并保存关注人到 bug_custom_field(fieldId='follower') */
    private void saveFollowerCustomField(String bugId, List<String> watcherUserKeys) {
        if (StringUtils.isBlank(bugId) || watcherUserKeys == null || watcherUserKeys.isEmpty()) return;
        List<String> systemUserIds = new ArrayList<>();
        for (String uk : watcherUserKeys) {
            String sid = resolveSystemUserIdFromFeishuUserKey(uk);
            if (StringUtils.isNotBlank(sid)) systemUserIds.add(sid);
        }
        if (systemUserIds.isEmpty()) return;
        String fieldId = "follower";
        try {
            String value = objectMapper.writeValueAsString(systemUserIds);
            BugCustomField existing = bugCustomFieldMapper.selectByPrimaryKey(bugId, fieldId);
            if (existing != null) {
                BugCustomField update = new BugCustomField();
                update.setBugId(bugId);
                update.setFieldId(fieldId);
                update.setValue(value);
                update.setContent(value);
                bugCustomFieldMapper.updateByPrimaryKeySelective(update);
            } else {
                BugCustomField insert = new BugCustomField();
                insert.setBugId(bugId);
                insert.setFieldId(fieldId);
                insert.setValue(value);
                insert.setContent(value);
                bugCustomFieldMapper.insertSelective(insert);
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 写入关注人自定义字段失败: bugId=" + bugId + ", e=" + e.getMessage());
        }
    }

    /** 从飞书详情 fields 中解析 multi_user 类型字段，返回 user_key 列表 */
    private List<String> parseMultiUserFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return Collections.emptyList();
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode u : fv) {
                    if (u.isTextual() && StringUtils.isNotBlank(u.asText(""))) {
                        result.add(u.asText(""));
                    } else if (u.isObject()) {
                        String uk = u.path("user_key").asText("");
                        if (StringUtils.isBlank(uk)) uk = u.path("id").asText("");
                        if (StringUtils.isNotBlank(uk)) result.add(uk);
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    /** 从飞书详情 fields 中解析 date 类型字段，返回毫秒时间戳 */
    private Long parseDateFieldFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return null;
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isNumber()) {
                long val = fv.asLong(0);
                if (val <= 0) return null;
                return val > 1_000_000_000_000L ? val : val * 1000;
            }
            if (fv.isTextual()) {
                String s = fv.asText("").trim();
                if (StringUtils.isBlank(s)) return null;
                try {
                    long val = Long.parseLong(s);
                    if (val <= 0) return null;
                    return val > 1_000_000_000_000L ? val : val * 1000;
                } catch (NumberFormatException ignored) {}
                try {
                    return java.time.LocalDate.parse(s)
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** 从飞书详情 fields 中解析 multi_select 类型字段，返回所有 label 用逗号拼接 */
    private String parseMultiSelectLabelsFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isArray() && fv.size() > 0) {
                List<String> labels = new ArrayList<>();
                for (JsonNode item : fv) {
                    if (item.isTextual() && StringUtils.isNotBlank(item.asText(""))) {
                        labels.add(item.asText("").trim());
                    } else if (item.isObject()) {
                        String label = item.path("label").asText("");
                        if (StringUtils.isNotBlank(label)) labels.add(label.trim());
                    }
                }
                return String.join(",", labels);
            }
        }
        return "";
    }

    /**
     * 从 tree_select 字段解析叶子节点的 label（沿 children 递归到底）。
     * 用于发现人等需要取最深层选项的字段。
     */
    private String parseTreeSelectLeafLabelFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isObject()) {
                JsonNode current = fv;
                while (current.isObject()) {
                    JsonNode children = current.path("children");
                    if (children.isMissingNode() || children.isNull() || !children.isObject()) break;
                    current = children;
                }
                String label = current.path("label").asText("");
                if (StringUtils.isNotBlank(label)) return label.trim();
                String value = current.path("value").asText("");
                if (StringUtils.isNotBlank(value)) return value.trim();
            }
        }
        return "";
    }

    /**
     * 从 tree_select 字段解析完整路径，格式为 "一级_二级"。
     * 如只有一级，返回一级名称（如 "环境问题"）；
     * 如有二级，返回 "一级_二级"（如 "漏测_用例未设计"），与前端 value 格式一致。
     */
    private String parseTreeSelectPathFromDetail(JsonNode detail, String... fieldKeys) {
        JsonNode fields = detail.path("fields");
        if (!fields.isArray()) return "";
        Set<String> keys = new HashSet<>(Arrays.asList(fieldKeys));
        for (JsonNode f : fields) {
            String fk = f.path("field_key").asText("");
            String alias = f.path("field_alias").asText("");
            if (!keys.contains(fk) && !keys.contains(alias)) continue;
            JsonNode fv = f.path("field_value");
            if (fv.isObject()) {
                List<String> labels = new ArrayList<>();
                JsonNode current = fv;
                while (current.isObject()) {
                    String label = current.path("label").asText("");
                    if (StringUtils.isNotBlank(label)) labels.add(label.trim());
                    JsonNode children = current.path("children");
                    if (children.isMissingNode() || children.isNull() || !children.isObject()) break;
                    current = children;
                }
                if (labels.isEmpty()) return "";
                if (labels.size() == 1) return labels.get(0);
                String parent = labels.get(0);
                String leaf = labels.get(labels.size() - 1);
                return parent.equals(leaf) ? parent : parent + "_" + leaf;
            }
        }
        return "";
    }

    private void handleDelete(String workItemId) {
        Bug existing = findByPlatformBugId(workItemId);
        if (existing != null) {
            bugMapper.deleteByPrimaryKey(existing.getId());
            bugContentMapper.deleteByPrimaryKey(existing.getId());
            LogUtils.info("[FeishuWebhook] 删除本地缺陷: bugId={}, workItemId={}", existing.getId(), workItemId);
        } else {
            LogUtils.info("[FeishuWebhook] handleDelete: workItemId={} 本地无对应缺陷，跳过", workItemId);
        }
    }

    /**
     * 通过飞书 storyId 查 test_plan 表获取所属 projectId。
     * 链路：飞书缺陷 → 关联需求(storyId) → test_plan.feishu_story_id → test_plan.project_id
     */
    public String resolveProjectIdByStoryId(String storyId) {
        if (StringUtils.isBlank(storyId)) {
            LogUtils.warn("[FeishuWebhook] 缺陷未关联需求，无法路由到项目");
            return null;
        }
        TestPlanExample example = new TestPlanExample();
        example.createCriteria().andFeishuStoryIdEqualTo(storyId);
        List<TestPlan> plans = testPlanMapper.selectByExample(example);
        if (!plans.isEmpty()) {
            String projectId = plans.get(0).getProjectId();
            LogUtils.info("[FeishuWebhook] 通过 storyId={} 找到 projectId={}", storyId, projectId);
            return projectId;
        }
        LogUtils.warn("[FeishuWebhook] storyId={} 未匹配到 test_plan，无法确定项目", storyId);
        return null;
    }

    /**
     * 按 projectId 查询该项目默认缺陷模板 ID，与 BugService 中用法一致。
     */
    private String resolveTemplateId(String projectId) {
        if (StringUtils.isBlank(projectId)) {
        return "";
        }
        try {
            List<ProjectTemplateOptionDTO> options = projectTemplateService.getOption(projectId, TemplateScene.BUG.name());
            String templateId = Stream.ofNullable(options)
                    .flatMap(Collection::stream)
                    .filter(ProjectTemplateOptionDTO::getEnableDefault)
                    .map(ProjectTemplateOptionDTO::getId)
                    .findFirst()
                    .orElse("");
            LogUtils.info("[FeishuWebhook] resolveTemplateId: projectId={}, templateId={}", projectId, templateId);
            return templateId;
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 解析项目默认缺陷模板失败: projectId={}, error={}", projectId, e.getMessage());
            return "";
        }
    }

    private Bug findByPlatformBugId(String platformBugId) {
        BugExample example = new BugExample();
        example.createCriteria()
                .andPlatformEqualTo(BugPlatform.FEISHU.getName())
                .andPlatformBugIdEqualTo(platformBugId);
        List<Bug> bugs = bugMapper.selectByExample(example);
        return bugs.isEmpty() ? null : bugs.get(0);
    }

    /**
     * 查找「我方刚创建、尚未回写 platform_bug_id」的缺陷，用于 Webhook 创建事件去重。
     * 条件：同项目、platform=FEISHU、platform_bug_id 为空、创建时间在 withinMillis 内，按创建时间倒序取第一条。
     */
    private Bug findRecentlyCreatedFeishuBugWithoutPlatformBugId(String projectId, long withinMillis) {
        if (StringUtils.isBlank(projectId)) {
            return null;
        }
        long since = System.currentTimeMillis() - withinMillis;
        BugExample example = new BugExample();
        example.setOrderByClause("create_time desc");
        example.createCriteria()
                .andPlatformEqualTo(BugPlatform.FEISHU.getName())
                .andProjectIdEqualTo(projectId)
                .andPlatformBugIdIsNull()
                .andCreateTimeGreaterThanOrEqualTo(since);
        List<Bug> bugs = bugMapper.selectByExample(example);
        return bugs.isEmpty() ? null : bugs.get(0);
    }

    /**
     * 从事件数据中提取关联的需求 ID（飞书 story work_item_id，与平台需求 id 一致时可用于路由项目）。
     * WorkitemCreateEvent 的关联通常在 fields / field_value_pairs 中，Update 可能在 field_changes。
     */
    @SuppressWarnings("unchecked")
    private String extractLinkedStoryId(Map<String, Object> event) {
        // 方式1：从 connections 字段提取
        Object connections = event.get("connections");
        if (connections instanceof List<?> connList) {
            for (Object conn : connList) {
                if (conn instanceof Map<?, ?> connMap) {
                    Object typeKeyObj = connMap.get("work_item_type_key");
                    if ("story".equalsIgnoreCase(typeKeyObj != null ? typeKeyObj.toString() : "")) {
                        Object workItemId = connMap.get("work_item_id");
                        if (workItemId != null) return workItemId.toString();
                    }
                }
            }
        }

        // 方式2：从 related_work_items 字段提取
        Object relatedItems = event.get("related_work_items");
        if (relatedItems instanceof List<?> relList) {
            for (Object rel : relList) {
                if (rel instanceof Map<?, ?> relMap) {
                    Object typeKeyObj = relMap.get("work_item_type_key");
                    if ("story".equalsIgnoreCase(typeKeyObj != null ? typeKeyObj.toString() : "")) {
                        Object workItemId = relMap.get("work_item_id");
                        if (workItemId != null) return workItemId.toString();
                    }
                }
            }
        }

        // 方式3：从 fields / field_value_pairs 提取（WorkitemCreateEvent 关联需求在这里）
        String fromFields = extractStoryIdFromFieldsOrValuePairs(event);
        if (StringUtils.isNotBlank(fromFields)) return fromFields;

        // 调试：有 fields 却未解析到 story 时打印 field_key 列表，便于确认飞书「关联需求」字段名
        Object fieldsObj = event.get("fields");
        if (fieldsObj instanceof List<?> fl && !fl.isEmpty()) {
            Set<String> keys = new java.util.LinkedHashSet<>();
            for (Object f : fl) {
                if (f instanceof Map<?, ?> fm && fm.get("field_key") != null) keys.add(String.valueOf(fm.get("field_key")));
            }
            if (!keys.isEmpty()) {
                LogUtils.info("[FeishuWebhook] 未从 fields 解析到关联需求，当前 event.fields 的 field_key 列表: {}", keys);
            }
        }

        // 方式4：从 field_changes 中提取关联需求字段
        Object fieldChanges = event.get("field_changes");
        if (fieldChanges instanceof List<?> changes) {
            for (Object change : changes) {
                if (change instanceof Map<?, ?> fieldMap) {
                    Object fieldKeyObj = fieldMap.get("field_key");
                    String fieldKey = fieldKeyObj != null ? fieldKeyObj.toString() : "";
                    if (FEISHU_FIELD_LINKED_STORY.equals(fieldKey) || FEISHU_FIELD_LINKED_STORY_ID.equals(fieldKey) || fieldKey.contains("story") || fieldKey.contains("requirement") || fieldKey.contains("connection") || fieldKey.contains("link")) {
                        Object newValue = fieldMap.get("new_value");
                        if (newValue != null) return parseStoryIdFromValue(newValue);
                    }
                }
            }
        }

        return "";
    }

    /** 从 fields 或 field_value_pairs 中找表示「关联需求」的字段，返回 story 的 work_item_id（字符串） */
    @SuppressWarnings("unchecked")
    private String extractStoryIdFromFieldsOrValuePairs(Map<String, Object> event) {
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (!(listObj instanceof List<?> list)) continue;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> fieldMap)) continue;
                Object keyObj = fieldMap.get("field_key");
                String fieldKey = keyObj != null ? String.valueOf(keyObj) : "";
                if (!isStoryRelatedFieldKey(fieldKey)) continue;
                Object fv = fieldMap.get("field_value");
                if (fv == null) continue;
                String sid = parseStoryIdFromValue(fv);
                if (StringUtils.isNotBlank(sid)) return sid;
            }
        }
        return "";
    }

    /** 飞书「关联需求」对接标识（字段管理 → 对接标识） */
    private static final String FEISHU_FIELD_LINKED_STORY = "_field_linked_story";
    /** 飞书「关联需求」自定义字段 ID（字段管理 → 字段ID），与 _field_linked_story 同义 */
    private static final String FEISHU_FIELD_LINKED_STORY_ID = "field_a62d41";

    private boolean isStoryRelatedFieldKey(String fieldKey) {
        if (StringUtils.isBlank(fieldKey)) return false;
        if (FEISHU_FIELD_LINKED_STORY.equals(fieldKey) || FEISHU_FIELD_LINKED_STORY_ID.equals(fieldKey)) return true;
        String k = fieldKey.toLowerCase();
        return k.contains("story") || k.contains("requirement") || k.contains("connection")
                || k.contains("link") || k.contains("需求") || k.contains("关联");
    }

    /** 将 field_value 解析为 story id 字符串：可能是字符串、或对象 {work_item_id}、或数组首元素 */
    @SuppressWarnings("unchecked")
    private String parseStoryIdFromValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            String s = ((String) value).trim();
            return StringUtils.isNotBlank(s) ? s : "";
        }
        if (value instanceof Map<?, ?> map) {
            Object wid = map.get("work_item_id");
            if (wid != null) return wid.toString().trim();
            Object id = map.get("id");
            if (id != null) return id.toString().trim();
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object wid = m.get("work_item_id");
                if (wid != null) return wid.toString().trim();
            }
            return first != null ? first.toString().trim() : "";
        }
        return "";
    }

    /**
     * 当 Webhook 事件里解析不到关联需求时，用飞书「工作项详情」接口拉取，从返回的 connections 取 story。
     * 飞书 Webhook 的 fields 里多为自定义 field_xxx，无法从名称识别；详情接口返回的 connections 是标准结构。
     */
    private String fetchLinkedStoryIdFromFeishuApi(String projectKey, String workItemId) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemId)) return "";
        long wid;
        try {
            wid = Long.parseLong(workItemId);
        } catch (NumberFormatException e) {
            LogUtils.warn("[FeishuWebhook] workItemId 非数字，无法调详情接口: workItemId={}", workItemId);
            return "";
        }
        try {
            LogUtils.info("[FeishuWebhook] 调飞书工作项详情取关联需求: projectKey={}, workItemId={}", projectKey, workItemId);
            List<JsonNode> list = feishuMeegoService.getDefectDetails(projectKey, null, Collections.singletonList(wid));
            if (list != null && !list.isEmpty()) {
                JsonNode first = list.get(0);
                String storyId = parseStoryIdFromDefectDetail(first);
                if (StringUtils.isBlank(storyId)) {
                    logDefectDetailStructureForDebug(first, workItemId);
                }
                return storyId;
            }
            LogUtils.info("[FeishuWebhook] 飞书工作项详情返回空列表: projectKey={}, workItemId={}", projectKey, workItemId);
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 调飞书工作项详情取关联需求失败: projectKey=" + projectKey + ", workItemId=" + workItemId + ", e=" + e.getMessage());
        }
        return "";
    }

    /** 调试用：当解析不到关联需求时打印详情结构，便于确认飞书 API 返回的字段位置 */
    private void logDefectDetailStructureForDebug(JsonNode item, String workItemId) {
        if (item == null) return;
        java.util.Iterator<String> it = item.fieldNames();
        List<String> topKeys = new ArrayList<>();
        while (it.hasNext()) topKeys.add(it.next());
        StringBuilder sb = new StringBuilder();
        sb.append("[FeishuWebhook] 飞书详情无 story，workItemId=").append(workItemId)
                .append(", 顶层 keys=").append(topKeys);
        JsonNode pairs = item.path("field_value_pairs");
        if (!pairs.isArray()) pairs = item.path("fields");
        if (pairs.isArray()) {
            sb.append(", field_value_pairs/fields 共 ").append(pairs.size()).append(" 项，前 15 项: ");
            int max = Math.min(15, pairs.size());
            for (int i = 0; i < max; i++) {
                JsonNode f = pairs.get(i);
                String key = f.path("field_key").asText("");
                JsonNode val = f.path("field_value");
                if (val.isMissingNode()) val = f.path("value");
                String valType = val.isMissingNode() ? "missing" : (val.isObject() ? "object" : val.isArray() ? "array(" + val.size() + ")" : "text");
                sb.append("[").append(key).append("=").append(valType);
                if (val.isObject()) {
                    java.util.Iterator<String> vi = val.fieldNames();
                    List<String> vKeys = new ArrayList<>();
                    while (vi.hasNext()) vKeys.add(vi.next());
                    sb.append(",keys=").append(vKeys);
                    if (val.has("work_item_id")) sb.append(",work_item_id=").append(val.path("work_item_id").asText(""));
                    if (val.has("work_item_type_key")) sb.append(",work_item_type_key=").append(val.path("work_item_type_key").asText(""));
                }
                sb.append("] ");
            }
        }
        LogUtils.info(sb.toString());
    }

    /** 从飞书工作项详情 JsonNode 中取关联的 story work_item_id。飞书 UI「关联需求」可能落在 connections 或 field_value_pairs 中（含自定义 field_xxx，值为对象 {work_item_id, name}）。 */
    private String parseStoryIdFromDefectDetail(JsonNode item) {
        if (item == null) return "";
        JsonNode connections = item.path("connections");
        if (connections.isArray()) {
            for (JsonNode conn : connections) {
                String typeKey = conn.path("work_item_type_key").asText("");
                if ("story".equalsIgnoreCase(typeKey)) {
                    return conn.path("work_item_id").asText("");
                }
            }
        }
        JsonNode pairs = item.path("field_value_pairs");
        if (!pairs.isArray()) pairs = item.path("fields");
        if (pairs.isArray()) {
            for (JsonNode field : pairs) {
                String key = field.path("field_key").asText("");
                if (!FEISHU_FIELD_LINKED_STORY.equals(key) && !FEISHU_FIELD_LINKED_STORY_ID.equals(key) && !key.contains("story") && !key.contains("connection") && !key.contains("link") && !key.contains("需求") && !key.contains("关联")) {
                    continue;
                }
                JsonNode val = field.path("field_value");
                if (val.isMissingNode()) val = field.path("value");
                if (val.isMissingNode()) continue;
                if (val.isArray() && val.size() > 0) val = val.get(0);
                if (val.isObject()) {
                    JsonNode wid = val.path("work_item_id");
                    if (wid.isMissingNode()) wid = val.path("id");
                    if (!wid.isMissingNode() && StringUtils.isNotBlank(wid.asText(""))) {
                        String typeKey = val.path("work_item_type_key").asText("");
                        if ("story".equalsIgnoreCase(typeKey)) return wid.asText("");
                        if (FEISHU_FIELD_LINKED_STORY.equals(key) || FEISHU_FIELD_LINKED_STORY_ID.equals(key) || key.contains("story") || key.contains("connection") || key.contains("link") || key.contains("需求") || key.contains("关联")) {
                            return wid.asText("");
                        }
                    }
                } else if (!val.isArray()) {
                    String s = val.asText("");
                    if (StringUtils.isNotBlank(s) && (FEISHU_FIELD_LINKED_STORY.equals(key) || FEISHU_FIELD_LINKED_STORY_ID.equals(key) || key.contains("story") || key.contains("connection"))) {
                        return s;
                    }
                }
            }
            for (JsonNode field : pairs) {
                JsonNode val = field.path("field_value");
                if (val.isMissingNode()) val = field.path("value");
                if (val.isArray() && val.size() > 0) val = val.get(0);
                if (val.isObject()) {
                    JsonNode wid = val.path("work_item_id");
                    if (!wid.isMissingNode() && StringUtils.isNotBlank(wid.asText(""))) {
                        if ("story".equalsIgnoreCase(val.path("work_item_type_key").asText(""))) return wid.asText("");
                    }
                }
            }
        }
        return "";
    }

    /** 从事件中取标题：先顶层 name，再 fields/field_value_pairs 中的 name */
    private String extractTitleFromEvent(Map<String, Object> event) {
        Object n = event.get("name");
        if (n != null && StringUtils.isNotBlank(String.valueOf(n))) return String.valueOf(n).trim();
        String fromFields = extractFieldValueFromEvent(event, "name");
        return StringUtils.isNotBlank(fromFields) ? fromFields : "";
    }

    /** 从事件中取状态：优先飞书对接标识 work_item_status，其次 status。飞书 state_key 多为英文（如 Not started），映射为系统侧枚举（待确认等）后落库。 */
    @SuppressWarnings("unchecked")
    private String extractStatusFromEvent(Map<String, Object> event) {
        String fromWs = parseStatusValue(event.get("work_item_status"));
        if (StringUtils.isNotBlank(fromWs)) return mapFeishuStatusToSystem(fromWs);
        String fromStatus = parseStatusValue(event.get("status"));
        if (StringUtils.isNotBlank(fromStatus)) return mapFeishuStatusToSystem(fromStatus);
        Object rawFromFields = getRawFieldValueFromEvent(event, "work_item_status", "status");
        String fromFields = parseStatusValue(rawFromFields);
        if (StringUtils.isNotBlank(fromFields)) return mapFeishuStatusToSystem(fromFields);
        Object rawFromChanges = getRawFieldValueFromFieldChanges(event, "work_item_status", "status");
        String fromChanges = parseStatusValue(rawFromChanges);
        if (StringUtils.isNotBlank(fromChanges)) return mapFeishuStatusToSystem(fromChanges);
        return "待确认";
    }

    /** 飞书 state_key 或中文 → 系统状态展示值（写死在 FeishuOptionMapping） */
    private static String mapFeishuStatusToSystem(String feishuStateKey) {
        return FeishuOptionMapping.toStatusDisplay(feishuStateKey);
    }

    /** 从 field_changes / changed_fields 中按 field_key（或 field_alias）取当前值。
     * 兼容两种结构：new_value（部分事件）或 cur_field_value（飞书 WorkitemUpdateEvent 等）。
     * 其它字段（状态、经办人、自定义字段等）统一用此方法即可，无需逐个加调试。 */
    @SuppressWarnings("unchecked")
    private Object getRawFieldValueFromFieldChanges(Map<String, Object> event, String... fieldKeys) {
        if (event == null || fieldKeys == null || fieldKeys.length == 0) return null;
        for (String changesKey : Arrays.asList("field_changes", "changed_fields")) {
            Object fieldChanges = event.get(changesKey);
            if (!(fieldChanges instanceof List<?> changes)) continue;
            for (Object change : changes) {
                if (change instanceof Map<?, ?> fieldMap) {
                    Object keyObj = fieldMap.get("field_key");
                    Object aliasObj = fieldMap.get("field_alias");
                    String key = keyObj != null ? String.valueOf(keyObj) : (aliasObj != null ? String.valueOf(aliasObj) : "");
                    for (String target : fieldKeys) {
                        if (target.equals(key)) {
                            Object newVal = fieldMap.get("new_value");
                            if (newVal != null) return newVal;
                            Object curVal = fieldMap.get("cur_field_value");
                            if (curVal != null) return curVal;
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 飞书状态值可能是对象 {state_key: "Not started", ...}，只取 state_key 等短字符串，避免超长写入 bug.status */
    private static final int STATUS_MAX_LENGTH = 64;

    private String parseStatusValue(Object value) {
        if (value == null) return "";
        if (value instanceof Map<?, ?> map) {
            Object sk = map.get("state_key");
            if (sk != null && StringUtils.isNotBlank(String.valueOf(sk))) return truncate(String.valueOf(sk).trim(), STATUS_MAX_LENGTH);
            Object s = map.get("state");
            if (s != null && StringUtils.isNotBlank(String.valueOf(s))) return truncate(String.valueOf(s).trim(), STATUS_MAX_LENGTH);
            Object k = map.get("key");
            if (k != null && StringUtils.isNotBlank(String.valueOf(k))) return truncate(String.valueOf(k).trim(), STATUS_MAX_LENGTH);
            return "";
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            return StringUtils.isNotBlank(s) ? truncate(s, STATUS_MAX_LENGTH) : "";
        }
        return "";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    /** 从 event 的 fields/field_value_pairs 中按 field_key 取原始 field_value（未转字符串），用于状态等可能为对象的字段 */
    @SuppressWarnings("unchecked")
    private Object getRawFieldValueFromEvent(Map<String, Object> event, String... fieldKeys) {
        if (event == null || fieldKeys == null || fieldKeys.length == 0) return null;
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        String key = keyObj != null ? String.valueOf(keyObj) : "";
                        for (String target : fieldKeys) {
                            if (target.equals(key)) {
                                Object fv = fieldMap.get("field_value");
                                if (fv != null) return fv;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 通用：从 event 的 field_changes（new_value）或 fields/field_value_pairs（field_value）中按 field_key 取值。
     * 支持多个候选 field_key（飞书不同空间可能命名不同），返回第一个非空值。
     */
    @SuppressWarnings("unchecked")
    private String extractFieldValueFromEvent(Map<String, Object> event, String... fieldKeys) {
        if (event == null || fieldKeys == null || fieldKeys.length == 0) return "";

        // 1. 从 field_changes / changed_fields 取 new_value
        for (String changesKey : Arrays.asList("field_changes", "changed_fields")) {
            Object fieldChanges = event.get(changesKey);
            if (fieldChanges instanceof List<?> changes) {
                for (Object change : changes) {
                    if (change instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        String key = keyObj != null ? String.valueOf(keyObj) : "";
                        for (String target : fieldKeys) {
                            if (target.equals(key)) {
                                Object newVal = fieldMap.get("new_value");
                                if (newVal != null) {
                                    String v = String.valueOf(newVal).trim();
                                    if (StringUtils.isNotBlank(v)) return v;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 2. 从 fields / field_value_pairs 取 field_value
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        String key = keyObj != null ? String.valueOf(keyObj) : "";
                        for (String target : fieldKeys) {
                            if (target.equals(key)) {
                                Object fv = fieldMap.get("field_value");
                                if (fv != null) {
                                    String v = String.valueOf(fv).trim();
                                    if (StringUtils.isNotBlank(v)) return v;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /** 描述：先 field_changes，再 fields/field_value_pairs（创建事件常只有后者） */
    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> event) {
        String fromFields = extractFieldValueFromEvent(event, "description", "desc");
        if (StringUtils.isNotBlank(fromFields)) return fromFields;
        Object fieldChanges = event.get("field_changes");
        if (fieldChanges instanceof List<?> changes) {
            for (Object change : changes) {
                if (change instanceof Map<?, ?> fieldMap) {
                    Object keyObj = fieldMap.get("field_key");
                    if ("description".equals(keyObj != null ? String.valueOf(keyObj) : "")) {
                        Object newValue = fieldMap.get("new_value");
                        return newValue != null ? String.valueOf(newValue) : "";
                    }
                }
            }
        }
        return "";
    }

    /**
     * 从飞书 Webhook 事件中提取处理人（对应飞书「经办人」）。
     * 飞书角色：报告人=reporter、经办人=operator（我们处理人）、产品经理=role_b9fbfb。
     * 优先从 event 顶层、field_changes、fields/field_value_pairs 中按 operator/current_status_operator/assignee/owner 取，否则用 updated_by。
     */
    private static final String FEISHU_FIELD_HANDLER = "operator"; // 飞书「经办人」角色id/对接标识 → 我们处理人

    @SuppressWarnings("unchecked")
    private String extractHandleUserFromEvent(Map<String, Object> event) {
        if (event == null) return "";

        // 1. event 顶层：经办人(operator)、当前负责人(current_status_operator)
        String fromTop = parseUserKeyFromValue(event.get(FEISHU_FIELD_HANDLER));
        if (StringUtils.isNotBlank(fromTop)) return fromTop;
        fromTop = parseUserKeyFromValue(event.get("current_status_operator"));
        if (StringUtils.isNotBlank(fromTop)) return fromTop;

        // 2. field_changes：经办人/当前负责人/assignee/owner
        Object fieldChanges = event.get("field_changes");
        if (fieldChanges instanceof List<?> changes) {
            for (Object change : changes) {
                if (change instanceof Map<?, ?> fieldMap) {
                    Object keyObj = fieldMap.get("field_key");
                    String fieldKey = keyObj != null ? String.valueOf(keyObj) : "";
                    if (FEISHU_FIELD_HANDLER.equals(fieldKey) || "current_status_operator".equals(fieldKey) || "assignee".equals(fieldKey) || "owner".equals(fieldKey)) {
                        Object newVal = fieldMap.get("new_value");
                        String v = parseUserKeyFromValue(newVal);
                        if (StringUtils.isNotBlank(v)) return v;
                    }
                }
            }
        }

        // 3. fields / field_value_pairs：经办人(operator)/当前负责人/assignee/owner
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        String fieldKey = keyObj != null ? String.valueOf(keyObj) : "";
                        if (FEISHU_FIELD_HANDLER.equals(fieldKey) || "current_status_operator".equals(fieldKey) || "assignee".equals(fieldKey) || "owner".equals(fieldKey)) {
                            Object fv = fieldMap.get("field_value");
                            String v = parseUserKeyFromValue(fv);
                            if (StringUtils.isNotBlank(v)) return v;
                        }
                    }
                }
            }
        }

        // 4. 兜底：最后更新人 updated_by
        String updatedBy = parseUserKeyFromValue(event.get("updated_by"));
        if (StringUtils.isNotBlank(updatedBy)) return updatedBy;

        return "";
    }

    /** 将飞书人员字段值解析为单个 user_key：支持字符串、多选人员数组（取第一个）、对象含 user_key */
    @SuppressWarnings("unchecked")
    private String parseUserKeyFromValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            String s = ((String) value).trim();
            return StringUtils.isNotBlank(s) ? s : "";
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object uk = m.get("user_key");
                if (uk != null) return String.valueOf(uk).trim();
                Object id = m.get("id");
                if (id != null) return String.valueOf(id).trim();
            }
            return parseUserKeyFromValue(first);
        }
        if (value instanceof Map<?, ?> map) {
            Object uk = map.get("user_key");
            if (uk != null) return String.valueOf(uk).trim();
            Object id = map.get("id");
            if (id != null) return String.valueOf(id).trim();
        }
        return "";
    }

    /** 从事件中取创建人（飞书 user_key）。飞书对接标识：创建者=owner，报告人=reporter，二者均可作为创建人来源。 */
    @SuppressWarnings("unchecked")
    private String extractCreatedByFromEvent(Map<String, Object> event) {
        if (event == null) return "";
        String fromOwner = parseUserKeyFromValue(event.get("owner"));
        if (StringUtils.isNotBlank(fromOwner)) return fromOwner;
        String fromCreatedBy = parseUserKeyFromValue(event.get("created_by"));
        if (StringUtils.isNotBlank(fromCreatedBy)) return fromCreatedBy;
        String fromReporter = parseUserKeyFromValue(event.get("reporter"));
        if (StringUtils.isNotBlank(fromReporter)) return fromReporter;
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        String fieldKey = keyObj != null ? String.valueOf(keyObj) : "";
                        if ("owner".equals(fieldKey) || "created_by".equals(fieldKey) || "reporter".equals(fieldKey)) {
                            String v = parseUserKeyFromValue(fieldMap.get("field_value"));
                            if (StringUtils.isNotBlank(v)) return v;
                        }
                    }
                }
            }
        }
        return "";
    }

    /** 从事件中取产品经理（飞书 role_b9fbfb）。用于写入缺陷模板中「产品经理」自定义字段（需配置 feishu.meego.product-manager-field-id）。 */
    @SuppressWarnings("unchecked")
    private String extractProductManagerFromEvent(Map<String, Object> event) {
        if (event == null) return "";
        String fromTop = parseUserKeyFromValue(event.get("role_b9fbfb"));
        if (StringUtils.isNotBlank(fromTop)) return fromTop;
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        if ("role_b9fbfb".equals(keyObj != null ? String.valueOf(keyObj) : "")) {
                            String v = parseUserKeyFromValue(fieldMap.get("field_value"));
                            if (StringUtils.isNotBlank(v)) return v;
                        }
                    }
                }
            }
        }
        return "";
    }

    /** 从事件中取优先级（飞书 priority，如 P0-P4）。用于写入缺陷模板中「优先级」自定义字段（需配置 feishu.meego.priority-field-id）。 */
    @SuppressWarnings("unchecked")
    private String extractPriorityFromEvent(Map<String, Object> event) {
        if (event == null) return "";
        // 1. 顶层 priority
        Object raw = event.get("priority");
        String v = parsePriorityValue(raw);
        if (StringUtils.isNotBlank(v)) return v;

        // 2. fields / field_value_pairs 中 field_key=priority
        raw = getRawFieldValueFromEvent(event, "priority");
        v = parsePriorityValue(raw);
        if (StringUtils.isNotBlank(v)) return v;

        // 3. field_changes / changed_fields 中 field_key=priority 的 new_value
        raw = getRawFieldValueFromFieldChanges(event, "priority");
        v = parsePriorityValue(raw);
        if (StringUtils.isNotBlank(v)) return v;

        // 4. 飞书 Webhook changed_fields 中 field_key=priority，结构为 cur_field_value: { label: "P1", value: "option_2" }
        for (String listKey : Arrays.asList("changed_fields", "update_ui_data")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Object uuid = m.get("uuid");
                        Object fieldKey = m.get("field_key");
                        String key = uuid != null ? String.valueOf(uuid) : (fieldKey != null ? String.valueOf(fieldKey) : "");
                        if (!"priority".equals(key)) continue;
                        Object curFieldValue = m.get("cur_field_value");
                        if (curFieldValue != null) {
                            String pv = parsePriorityValue(curFieldValue);
                            if (StringUtils.isNotBlank(pv)) return pv;
                        }
                        Object updateValue = m.get("update_value");
                        if (updateValue instanceof Map<?, ?> uv) {
                            Object select = uv.get("select");
                            if (select instanceof Map<?, ?> sel) {
                                Object selValue = sel.get("value");
                                String pv = parsePriorityValue(selValue);
                                if (StringUtils.isNotBlank(pv)) return pv;
                            }
                        }
                    }
                }
            }
        }

        // 5. 兜底：通用解析（字符串化）
        return extractFieldValueFromEvent(event, "priority");
    }

    /** 飞书优先级（选项 id 或 P0-P4）→ 系统展示值（写死在 FeishuOptionMapping） */
    private String mapFeishuPriorityToSystem(String feishuPriority) {
        return FeishuOptionMapping.toPriorityDisplay(feishuPriority);
    }

    /** 优先级可能为字符串 "P4" 或选项 id "r1ozs01jx" 或对象 { value, option_value, key } */
    @SuppressWarnings("unchecked")
    private String parsePriorityValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            String s = ((String) value).trim();
            return StringUtils.isNotBlank(s) ? s : "";
        }
        if (value instanceof Map<?, ?> map) {
            for (String key : Arrays.asList("value", "option_value", "key", "text")) {
                Object o = map.get(key);
                if (o != null && StringUtils.isNotBlank(String.valueOf(o))) return String.valueOf(o).trim();
            }
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return parsePriorityValue(list.get(0));
        }
        return "";
    }

    /** 若配置了优先级自定义字段 id，将飞书 priority 经 FeishuOptionMapping 转为 P0-P4 后写入 bug_custom_field */
    private void savePriorityCustomField(String bugId, String priorityValue) {
        if (StringUtils.isBlank(bugId)) return;
        if (StringUtils.isBlank(priorityValue)) return;
        String valueToStore = mapFeishuPriorityToSystem(priorityValue.trim());
        if (StringUtils.isBlank(priorityFieldId)) {
            LogUtils.info("[FeishuWebhook] 已解析优先级 raw={} -> {}，未配置 feishu.meego.priority-field-id，未写入自定义字段；请在缺陷模板中确认「优先级」字段 id 并配置", priorityValue, valueToStore);
            return;
        }
        try {
            BugCustomField existing = bugCustomFieldMapper.selectByPrimaryKey(bugId, priorityFieldId);
            String value = valueToStore;
            String content = value;
            if (existing != null) {
                BugCustomField update = new BugCustomField();
                update.setBugId(bugId);
                update.setFieldId(priorityFieldId);
                update.setValue(value);
                update.setContent(content);
                bugCustomFieldMapper.updateByPrimaryKeySelective(update);
            } else {
                BugCustomField insert = new BugCustomField();
                insert.setBugId(bugId);
                insert.setFieldId(priorityFieldId);
                insert.setValue(value);
                insert.setContent(content);
                bugCustomFieldMapper.insertSelective(insert);
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 写入优先级自定义字段失败: bugId=" + bugId + ", fieldId=" + priorityFieldId + ", e=" + e.getMessage());
        }
    }

    /**
     * 通用：按配置 feishu.meego.xxx-mapping 将飞书字段值映射为平台枚举值。
     * 配置格式：source1:target1,source2:target2；未匹配到时返回原值。
     */
    /** 飞书缺陷类型（API 可能返回 id 或中文）→ 飞书选项 ID 落库 */
    private String mapFeishuDefectType(String rawDefectType) {
        return FeishuOptionMapping.toDefectTypeId(rawDefectType);
    }

    /** 飞书发现人（API 可能返回 id 或中文）→ 飞书选项 ID 落库 */
    private String mapFeishuDiscoverer(String rawDiscoverer) {
        return FeishuOptionMapping.toDiscovererId(rawDiscoverer);
    }

    /** 从事件中取最后更新人（飞书 user_key）。飞书对接标识「更新人」为 updated_by。 */
    @SuppressWarnings("unchecked")
    private String extractUpdatedByFromEvent(Map<String, Object> event) {
        if (event == null) return "";
        String v = parseUserKeyFromValue(event.get("updated_by"));
        if (StringUtils.isNotBlank(v)) return v;
        for (String listKey : Arrays.asList("fields", "field_value_pairs")) {
            Object listObj = event.get(listKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object keyObj = fieldMap.get("field_key");
                        if ("updated_by".equals(keyObj != null ? String.valueOf(keyObj) : "")) {
                            v = parseUserKeyFromValue(fieldMap.get("field_value"));
                            if (StringUtils.isNotBlank(v)) return v;
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * 飞书 user_key → 本系统用户 ID。
     * 1) 用邮箱做中间值：飞书 user_key → FeishuMeegoService.getEmailByUserKey → 本系统按 email 查用户 id（系统邮箱与飞书一致时生效）；
     * 2) 配置映射 feishu.meego.user-key-to-system-user-id（格式 key1:id1,key2:id2）；
     * 3) 将 feishuUserKey 当作本系统用户 id 或 email 直接查询；
     * 否则返回空，调用方用 admin 等兜底。
     */
    private String resolveSystemUserIdFromFeishuUserKey(String feishuUserKey) {
        if (StringUtils.isBlank(feishuUserKey)) return "";

        // 1. 邮箱作为中间值：飞书 user_key → 邮箱 → 本系统用户 id（系统与飞书邮箱一致时可用）
        try {
            String email = feishuMeegoService.getEmailByUserKey(feishuUserKey);
            if (StringUtils.isNotBlank(email)) {
                List<UserExcludeOptionDTO> list = baseUserMapper.selectUserOptionByIdOrEmail(Collections.singletonList(email));
                if (list != null && list.size() == 1 && StringUtils.isNotBlank(list.get(0).getId())) {
                    return list.get(0).getId();
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 飞书 user_key→邮箱→本系统用户 解析失败: feishuUserKey={}, e={}", feishuUserKey, e.getMessage());
        }

        // 2. 配置映射
        if (StringUtils.isNotBlank(userKeyToSystemUserIdMapping)) {
            for (String pair : userKeyToSystemUserIdMapping.split(",")) {
                String[] kv = pair.trim().split(":", 2);
                if (kv.length == 2 && feishuUserKey.equals(kv[0].trim())) {
                    String id = kv[1].trim();
                    if (StringUtils.isNotBlank(id)) return id;
                }
            }
        }

        // 3. 将 feishuUserKey 视为本系统用户 id 或 email 直接查询
        try {
            List<UserExcludeOptionDTO> list = baseUserMapper.selectUserOptionByIdOrEmail(Collections.singletonList(feishuUserKey));
            if (list != null && list.size() == 1 && StringUtils.isNotBlank(list.get(0).getId())) {
                return list.get(0).getId();
            }
        } catch (Exception e) {
            LogUtils.warn("[FeishuWebhook] 按 id/email 解析飞书用户失败: feishuUserKey={}, e={}", feishuUserKey, e.getMessage());
        }

        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractString(Map<String, Object> payload, String... keys) {
        Object current = payload;
        for (String key : keys) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                return "";
            }
        }
        return current != null ? String.valueOf(current) : "";
    }
}
