package io.vanguard.testops.functional.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanguard.testops.functional.domain.MeegoStoryStats;
import io.vanguard.testops.functional.mapper.MeegoStoryStatsMapper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 飞书Meego需求集成服务
 * 用于同步飞书需求(Story)数据，包括需求关联的缺陷数量等信息
 */
@Service
public class FeishuMeegoService {

    @Resource
    private MeegoStoryStatsMapper meegoStoryStatsMapper;

    public List<MeegoStoryStats> searchStories(String keyword) {
        return meegoStoryStatsMapper.selectByStoryNameLike(keyword);
    }

    // 回归使用 Spring 注入的 RestTemplate
    @Resource
    @org.springframework.beans.factory.annotation.Qualifier("feishuRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${feishu.meego.base-url:https://project.feishu.cn}")
    private String baseUrl;

    @Value("${feishu.meego.plugin-id:}")
    private String pluginId;

    @Value("${feishu.meego.plugin-secret:}")
    private String pluginSecret;

    @Value("${feishu.meego.default-user-email:}")
    private String defaultUserEmail;

    @Value("${feishu.meego.project-key:}")
    private String defaultProjectKey;

    @Value("${feishu.meego.defect-type-key:63329b6c980d67099b12fd73}")
    private String defectTypeKey;

    private String cachedPluginToken;
    private long tokenExpireTime = 0;
    private final Map<String, String> userKeyCache = new ConcurrentHashMap<>();
    /** 缺陷类型字段 key 缓存：用于 create 时只传飞书支持的字段，并取 field_type_key 做入参格式 */
    private volatile String defectFieldKeysProjectKey = null;
    private volatile long defectFieldKeysExpire = 0;
    private volatile Set<String> defectFieldKeysCache = null;
    /** field_key -> field_type_key（与 defectFieldKeysCache 同周期刷新），用于按类型格式化 field_value 并传入 field_type_key */
    private volatile Map<String, String> defectFieldTypeCache = null;
    /** field_key -> value_type（来自「获取创建工作项元数据」GET .../meta），用于按官方 字段与属性解析格式 序列化 field_value，见 https://project.feishu.cn/b/helpcenter/1p8d7djs/1tj6ggll */
    private volatile Map<String, Integer> defectFieldValueTypeCache = null;
    /** 当前空间「优先级」字段的 label→value 映射（如 P0→option_xxx），避免管理员变更选项后 20050 option value expired */
    private volatile Map<String, String> priorityLabelToOptionValueCache = null;
    /** 当前空间「缺陷原因」字段(field_e84b00)的 label→value 映射，避免 20050 option value expired */
    private volatile Map<String, String> defectReasonLabelToOptionValueCache = null;
    /** 当前空间所有带 options 的字段的 label→value 缓存（field_key → (label→value)），统一避免 20050/选项过期 */
    private volatile Map<String, Map<String, String>> fieldOptionLabelToValueCaches = null;
    /** 流程状态：展示名(name)→state_key，来自「获取流程模板配置详情」workflow_confs，用于 work_item_status 更新（系统计算字段） */
    private volatile Map<String, String> workflowStateNameToKeyCache = null;
    private volatile String workflowStateCacheProjectKey = null;
    private volatile long workflowStateCacheExpire = 0;
    /** 飞书 user_key → email，用于 Webhook 侧用邮箱查本系统用户（与 userKeyCache 反向） */
    private final Map<String, String> userKeyToEmailCache = new ConcurrentHashMap<>();

    /**
     * 【关键】兼容解析响应码
     * Meego 接口有时返回 err_code，有时返回 code
     */
    private int getResponseCode(JsonNode root) {
        if (root.has("err_code")) return root.path("err_code").asInt(-1);
        if (root.has("code")) return root.path("code").asInt(-1);
        if (root.has("error") && root.path("error").has("code")) return root.path("error").path("code").asInt(-1);
        return -1;
    }

    private String getErrorMsg(JsonNode root) {
        if (root.has("err_msg")) return root.path("err_msg").asText();
        if (root.has("msg")) return root.path("msg").asText();
        if (root.has("error")) return root.path("error").path("msg").asText();
        return "未知错误";
    }

    /**
     * 获取 Plugin Token
     */
    public String getPluginToken() {
        if (cachedPluginToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedPluginToken;
        }
        String url = baseUrl + "/open_api/authen/plugin_token";
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("plugin_id", pluginId);
            requestBody.put("plugin_secret", pluginSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(response.getBody())) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (getResponseCode(root) == 0) {
                    String token = root.path("data").path("token").asText();
                    if (StringUtils.isBlank(token)) {
                        token = root.path("data").path("plugin_token").asText();
                    }

                    if (StringUtils.isBlank(token)) {
                        throw new MSException("获取Token失败: 返回体中缺少 token/plugin_token");
                    }

                    // 文档：plugin_token 有效期 7200 秒（2 小时）；若返回体中提供 expire_time，则优先使用返回值
                    long expiresInSeconds = root.path("data").path("expire_time").asLong(7200L);
                    if (expiresInSeconds <= 0) {
                        expiresInSeconds = 7200L;
                    }
                    // 提前 60 秒刷新，避免边界时刻 401
                    long safetySeconds = 60L;
                    long now = System.currentTimeMillis();
                    tokenExpireTime = now + Math.max(0, (expiresInSeconds - safetySeconds)) * 1000;
                    cachedPluginToken = token;
                    return cachedPluginToken;
                }
                throw new MSException("获取Token失败: " + getErrorMsg(root));
            }
            throw new MSException("HTTP异常: " + response.getStatusCode());
        } catch (Exception e) {
            throw new MSException("获取PluginToken异常: " + e.getMessage());
        }
    }

    /** 飞书侧无 admin 账号时，用此邮箱替代以获取 UserKey（仅用于调用飞书 Open API）。 */
    private static final String FEISHU_FALLBACK_EMAIL = "fred.fan@spotterio.com";
    private static final String ADMIN_EMAIL_TO_REPLACE = "admin@metersphere.io";

    /**
     * 解析调用飞书 API 使用的邮箱：未传时使用配置的 default-user-email（系统侧邮箱与飞书一致）。
     * 若解析结果为 admin@metersphere.io（飞书侧通常无此用户），则替换为 fred.fan@spotterio.com 以便正常调用飞书 API。
     */
    private String resolveUserEmail(String userEmail) {
        String effective = StringUtils.isNotBlank(userEmail) ? userEmail : defaultUserEmail;
        if (StringUtils.isBlank(effective)) {
            throw new MSException("未配置 feishu.meego.default-user-email 且未传入 userEmail，无法获取飞书 UserKey");
        }
        if (ADMIN_EMAIL_TO_REPLACE.equalsIgnoreCase(effective)) {
            return FEISHU_FALLBACK_EMAIL;
        }
        return effective;
    }

    /**
     * 通过邮箱获取飞书 user_key（用于 operator、reporter 等）。若飞书侧无该用户（如 User Not Found 30006）则返回 null，不抛异常。
     * 若传入邮箱为 admin@metersphere.io（飞书侧通常无此用户），则自动改用 fred.fan@spotterio.com 查询，与 resolveUserEmail 行为一致。
     */
    public String getUserKeyByEmail(String email) {
        if (StringUtils.isBlank(email)) return null;
        if (ADMIN_EMAIL_TO_REPLACE.equalsIgnoreCase(email)) {
            email = FEISHU_FALLBACK_EMAIL;
        }
        if (userKeyCache.containsKey(email)) return userKeyCache.get(email);

        String url = baseUrl + "/open_api/user/query";
        try {
            String pluginToken = getPluginToken();
            Map<String, Object> body = new HashMap<>();
            body.put("emails", Collections.singletonList(email));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", pluginToken);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(responseBody)) {
                JsonNode root = objectMapper.readTree(responseBody);
                if (getResponseCode(root) == 0) {
                    JsonNode dataNode = root.path("data");
                    String userKey = null;
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        userKey = dataNode.get(0).path("user_key").asText("");
                    } else if (dataNode.has("user_key")) {
                        userKey = dataNode.path("user_key").asText("");
                    }
                    if (StringUtils.isNotBlank(userKey)) {
                        userKeyCache.put(email, userKey);
                        userKeyToEmailCache.put(userKey, email);
                        return userKey;
                    }
                }
                return null;
            }
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 400 User Not Found (30006) 等：该邮箱在飞书不存在，返回 null，由调用方跳过该字段
            String body = e.getResponseBodyAsString();
            if (StringUtils.isNotBlank(body)) {
                try {
                    JsonNode root = objectMapper.readTree(body);
                    int code = getResponseCode(root);
                    if (code == 30006) {
                        LogUtils.info("[Feishu] getUserKeyByEmail 飞书无此用户: email={}, err_code=30006", email);
                        return null;
                    }
                } catch (Exception ignored) { }
            }
            LogUtils.warn("[Feishu] getUserKeyByEmail 请求失败: email={}, status={}", email, e.getStatusCode());
            return null;
        } catch (Exception e) {
            LogUtils.warn("[Feishu] getUserKeyByEmail 异常: email={}, e={}", email, e.getMessage());
            return null;
        }
    }

    /**
     * 通过飞书 user_key 获取邮箱（系统侧邮箱与飞书一致时，可用于查本系统用户 id）。
     * 先查缓存（getUserKeyByEmail 调用时会写入），再尝试 open_api/user/query 的 user_keys 参数。
     */
    public String getEmailByUserKey(String userKey) {
        if (StringUtils.isBlank(userKey)) return "";
        if (userKeyToEmailCache.containsKey(userKey)) {
            return userKeyToEmailCache.get(userKey);
        }
        String url = baseUrl + "/open_api/user/query";
        try {
            String pluginToken = getPluginToken();
            Map<String, Object> body = new HashMap<>();
            body.put("user_keys", Collections.singletonList(userKey));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", pluginToken);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(response.getBody())) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode dataNode = root.path("data");
                    String email = null;
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        JsonNode first = dataNode.get(0);
                        if (first.has("email")) email = first.path("email").asText("");
                        else if (first.has("account")) email = first.path("account").asText("");
                    } else if (dataNode.has("email")) {
                        email = dataNode.path("email").asText("");
                    } else if (dataNode.has("account")) {
                        email = dataNode.path("account").asText("");
                    }
                    if (StringUtils.isNotBlank(email)) {
                        userKeyToEmailCache.put(userKey, email);
                        return email;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] getEmailByUserKey 失败: userKey={}, e={}", userKey, e.getMessage());
        }
        return "";
    }



    // 常量定义
    private static final String STORY_TYPE_KEY = "story";
    private static final String DEFECT_TYPE_KEY = "63329b6c980d67099b12fd73"; // Defect 的真实 Type Key
    
    /**
     * 核心同步入口（异步执行，不阻塞主线程）
     * 逻辑源自用户提供的验证版本，适配 MeegoStoryStats 表
     */
    @Async("feishuSyncExecutor")
    public void syncStoriesAsync(String userEmail) {
        syncStoriesInternal(userEmail);
    }
    
    /**
     * 核心同步入口（同步方法，用于兼容现有调用）
     * 实际执行通过异步方法
     */
    public void syncStories(String userEmail) {
        syncStoriesAsync(userEmail);
    }
    
    /**
     * 核心同步逻辑实现
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    private void syncStoriesInternal(String userEmail) {
        LogUtils.info("========== 飞书需求同步任务开始 ==========");
        LogUtils.info("目标用户: {}", userEmail);
        try {
            String userKey = getUserKeyByEmail(userEmail);
            String token = getPluginToken();

            // 1. 获取所有需求 (Story) - 使用跨项目接口
            LogUtils.info(">>> [Step 1] 开始拉取所有【需求 (Story)】...");
            List<JsonNode> stories = fetchAllWorkItems(token, userKey, STORY_TYPE_KEY);
            LogUtils.info(">>> [Step 1] 需求拉取结束，总计: {} 条", stories.size());

            // 收集所有涉及的项目 Key
            Set<String> projectKeys = stories.stream()
                    .map(s -> s.path("project_key").asText())
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            LogUtils.info(">>> [Step 1] 涉及项目数: {}", projectKeys.size());

            // 初始化统计对象 Map
            Map<String, MeegoStoryStats> statsMap = new HashMap<>();
            
            for (JsonNode story : stories) {
                String storyId = story.path("id").asText();
                String storyName = story.path("name").asText();
                
                if (StringUtils.isBlank(storyId)) continue;
                
                MeegoStoryStats stats = new MeegoStoryStats();
                stats.setStoryId(storyId);
                stats.setStoryName(storyName);
                stats.setDefectCount(0);
                stats.setTestAnalysisTime(0.0);
                
                statsMap.put(storyId, stats);
                
                // 获取测分设计估时
                // 逻辑：从 state_times 中查找"测分设计"节点，获取 state_key，再查询详情
                JsonNode stateTimes = story.path("state_times");
                if (stateTimes.isArray()) {
                    for (JsonNode stateTime : stateTimes) {
                        String nodeName = stateTime.path("name").asText();
                        String stateKey = stateTime.path("state_key").asText();
                        
                        // 匹配节点名称（包含"测分设计"）
                        if (nodeName.contains("测分设计")) {
                            String projectKey = story.path("project_key").asText();
                            double nodePoints = fetchNodePoints(token, userKey, projectKey, 
                                "story", storyId, stateKey);
                            
                            if (nodePoints > 0) {
                                stats.setTestAnalysisTime(nodePoints);
                            }
                            break;
                        }
                    }
                }
            }

             // 2. 拉取 Defect 并聚合（优化版：流式处理+限流）
            // 性能优化说明：
            // - 流式处理：每次只处理一页数据（50条），处理完立即释放内存
            // - 分批限流：每批次之间暂停50ms，避免CPU持续满载
            // - 项目间限流：每个项目之间暂停100ms，给CPU降温
            LogUtils.info(">>> [Step 2] 开始拉取并聚合 Defect（流式处理优化）...");
            LogUtils.info("待处理项目数: {}, 单项目(3万缺陷)预计60-90秒", projectKeys.size());
            
            int totalDefects = 0;
            int processedProjects = 0;
            
            for (String pKey : projectKeys) {
                processedProjects++;
                LogUtils.info("[{}/{}] 开始处理项目: {}", processedProjects, projectKeys.size(), pKey);
                
                // 流式分页处理，不一次性加载全部缺陷到内存
                int projectDefectCount = processDefectsInStream(token, userKey, pKey, statsMap);
                totalDefects += projectDefectCount;
                
                LogUtils.info("项目 {} 完成，缺陷数: {}", pKey, projectDefectCount);
                
                // 项目间限流：减少到50ms（因为改为凌晨执行，可以适当提速）
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogUtils.warn("任务被中断");
                }
            }
            LogUtils.info(">>> [Step 2] Defect 处理结束，总计: {} 条", totalDefects);

            // 3. 入库保存
            LogUtils.info(">>> [Step 3] 开始保存到数据库...");
            for (MeegoStoryStats stats : statsMap.values()) {
                saveToDb(stats);
            }
            LogUtils.info(">>> [Step 3] 数据保存完成");

        } catch (Exception e) {
            LogUtils.error(">>> [Error] 同步失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 跨项目获取所有工作项
     */
    private List<JsonNode> fetchAllWorkItems(String token, String userKey, String typeKey) {
        List<JsonNode> allItems = new ArrayList<>();
        String url = baseUrl + "/open_api/work_items/filter_across_project";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-PLUGIN-TOKEN", token);
        headers.set("X-USER-KEY", userKey);

        int pageNum = 1;
        int pageSize = 50;
        boolean hasMore = true;

        while (hasMore) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("work_item_type_key", typeKey);
                body.put("page_num", pageNum);
                body.put("page_size", pageSize);
                
                ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

                if (resp.getStatusCode() == HttpStatus.OK) {
                    JsonNode root = objectMapper.readTree(resp.getBody());
                    if (getResponseCode(root) == 0) {
                        JsonNode data = root.path("data");
                        if (data.isArray() && data.size() > 0) {
                            for (JsonNode item : data) {
                                allItems.add(item);
                            }
                            pageNum++;
                        } else {
                            hasMore = false;
                        }
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                LogUtils.warn("Fetch Exception: {}", e.getMessage());
                hasMore = false;
            }
        }
        return allItems;
    }

     /**
      * 单项目获取工作项
      * 修正 URL: /open_api/{project_key}/work_item/filter
      */
     private List<JsonNode> fetchWorkItemsByProject(String token, String userKey, String projectKey, String typeKey) {
         List<JsonNode> allItems = new ArrayList<>();
         // 修正 URL: 必须包含 project_key，且是 work_item (单数)
         String url = baseUrl + "/open_api/" + projectKey + "/work_item/filter";

         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         headers.set("X-PLUGIN-TOKEN", token);
         headers.set("X-USER-KEY", userKey);

         int pageNum = 1;
         int pageSize = 50;
         boolean hasMore = true;

         while (hasMore) {
             try {
                 Map<String, Object> body = new HashMap<>();
                 // 单项目接口参数是 work_item_type_keys (复数)
                 body.put("work_item_type_keys", Collections.singletonList(typeKey));
                 body.put("page_num", pageNum);
                 body.put("page_size", pageSize);

                 ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

                 if (resp.getStatusCode() == HttpStatus.OK) {
                     JsonNode root = objectMapper.readTree(resp.getBody());
                     if (getResponseCode(root) == 0) {
                         JsonNode data = root.path("data");
                         if (data.isArray() && data.size() > 0) {
                             for (JsonNode item : data) {
                                 allItems.add(item);
                             }
                             pageNum++;
                             // 如果返回的数据少于 pageSize，说明已经是最后一页
                             if (data.size() < pageSize) {
                                 hasMore = false;
                             }
                         } else {
                             hasMore = false;
                         }
                     } else {
                         hasMore = false;
                     }
                 } else {
                     hasMore = false;
                 }
             } catch (Exception e) {
                 hasMore = false;
             }
         }
         return allItems;
     }

    /**
     * 流式处理缺陷数据（性能优化版）
     * 
     * 优化点：
     * 1. 分页处理：每次只加载50条数据到内存，处理完立即释放
     * 2. 即时聚合：边拉取边聚合，无需存储全部数据
     * 3. 批次限流：每批次之间暂停50ms，降低CPU峰值
     * 
     * @param token 认证Token
     * @param userKey 用户Key
     * @param projectKey 项目Key
     * @param statsMap 需求统计Map（用于聚合缺陷数）
     * @return 处理的缺陷总数
     */
    private int processDefectsInStream(String token, String userKey, String projectKey, 
                                        Map<String, MeegoStoryStats> statsMap) {
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/filter";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-PLUGIN-TOKEN", token);
        headers.set("X-USER-KEY", userKey);
        
        int pageNum = 1;
        int pageSize = 200;  // 优化：200条平衡性能和稳定性（3万缺陷 / 200 = 150次，约60-90秒完成）
        int totalProcessed = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                // 1. 拉取一页数据
                Map<String, Object> body = new HashMap<>();
                body.put("work_item_type_keys", Collections.singletonList(DEFECT_TYPE_KEY));
                body.put("page_num", pageNum);
                body.put("page_size", pageSize);
                
                ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
                
                if (resp.getStatusCode() == HttpStatus.OK) {
                    JsonNode root = objectMapper.readTree(resp.getBody());
                    if (getResponseCode(root) == 0) {
                        JsonNode data = root.path("data");
                        
                        if (data.isArray() && data.size() > 0) {
                            // 2. 立即处理这一页数据（边拉边处理，无需存储）
                            for (JsonNode defect : data) {
                                String linkedStoryId = extractFieldAsString(defect, "field_a62d41");
                                
                                if (StringUtils.isNotBlank(linkedStoryId) 
                                    && statsMap.containsKey(linkedStoryId)) {
                                    MeegoStoryStats stats = statsMap.get(linkedStoryId);
                                    stats.setDefectCount(stats.getDefectCount() + 1);
                                }
                            }
                            
                            totalProcessed += data.size();
                            pageNum++;
                            
                            // 3. 批次限流：10ms轻量级限流，平衡速度和稳定性
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            // 如果返回数据少于pageSize，说明是最后一页
                            if (data.size() < pageSize) {
                                hasMore = false;
                            }
                        } else {
                            hasMore = false;
                        }
                    } else {
                        LogUtils.warn("获取项目 {} 缺陷失败: {}", projectKey, getErrorMsg(root));
                        hasMore = false;
                    }
                } else {
                    LogUtils.warn("获取项目 {} 缺陷HTTP异常: {}", projectKey, resp.getStatusCode());
                    hasMore = false;
                }
            } catch (Exception e) {
                LogUtils.error("处理项目 {} 的缺陷时发生异常: {}", projectKey, e.getMessage());
                hasMore = false;
            }
        }
        
        return totalProcessed;
    }

    /**
     * 获取节点的points估分值
     */
    private double fetchNodePoints(String token, String userKey, String projectKey, 
                                    String workItemTypeKey, String workItemId, String targetNodeId) {
        try {
            String url = String.format("%s/open_api/%s/work_item/%s/query",
                baseUrl, projectKey, workItemTypeKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            Map<String, Object> body = new HashMap<>();
            body.put("work_item_ids", Collections.singletonList(Long.parseLong(workItemId)));
            
            Map<String, Boolean> expand = new HashMap<>();
            expand.put("need_workflow", true);
            body.put("expand", expand);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            
            if (resp.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    if (data.isArray() && data.size() > 0) {
                        JsonNode workItem = data.get(0);
                        
                        JsonNode workflowInfos = workItem.path("workflow_infos");
                        if (workflowInfos.isMissingNode()) {
                            workflowInfos = workItem.path("workflow");
                        }
                        
                        if (!workflowInfos.isMissingNode()) {
                            JsonNode nodes = workflowInfos.path("workflow_nodes");
                            if (nodes.isMissingNode()) {
                                nodes = workflowInfos.path("nodes");
                            }
                            
                            if (nodes.isArray()) {
                                for (JsonNode node : nodes) {
                                    String nodeId = node.path("id").asText();
                                    if (nodeId.isEmpty()) nodeId = node.path("node_id").asText();
                                    
                                    String stateKey = node.path("state_key").asText();
                                    
                                    if (targetNodeId.equals(nodeId) || targetNodeId.equals(stateKey)) {
                                        double finalPoints = 0.0;
                                        
                                        // 1. 尝试从 node_schedule 提取
                                        JsonNode nodeSchedule = node.path("node_schedule");
                                        if (nodeSchedule.has("points")) {
                                            finalPoints = nodeSchedule.path("points").asDouble(0.0);
                                        }
                                        
                                        // 2. 如果为0，尝试从 schedules 数组累加
                                        if (finalPoints == 0.0) {
                                            JsonNode schedules = node.path("schedules");
                                            if (schedules.isArray() && schedules.size() > 0) {
                                                double totalPoints = 0.0;
                                                for (JsonNode schedule : schedules) {
                                                    totalPoints += schedule.path("points").asDouble(0.0);
                                                }
                                                finalPoints = totalPoints;
                                            }
                                        }
                                        
                                        return finalPoints;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn("获取节点points失败: {}", e.getMessage());
        }
        
        return 0.0;
    }

    private String extractFieldAsString(JsonNode workItem, String targetFieldKey) {
        JsonNode fields = workItem.path("fields");
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                if (targetFieldKey.equals(field.path("field_key").asText())) {
                    JsonNode value = field.path("field_value");
                    if (value.isNumber()) {
                        return String.valueOf(value.asLong());
                    }
                    return value.asText();
                }
            }
        }
        return "";
    }

    private void saveToDb(MeegoStoryStats stats) {
        try {
            MeegoStoryStats existing = meegoStoryStatsMapper.selectByStoryId(stats.getStoryId());
            
            stats.setUpdatedAt(new Date());
            
            if (existing != null) {
                stats.setId(existing.getId()); // 保持 ID 不变
                meegoStoryStatsMapper.updateByPrimaryKey(stats);
            } else {
                meegoStoryStatsMapper.insert(stats);
            }
            
            if (stats.getDefectCount() > 0 || stats.getTestAnalysisTime() > 0) {
                LogUtils.info(">>> [DB] 已同步: {} (缺陷: {}, 测分: {})",
                        stats.getStoryName(), stats.getDefectCount(), stats.getTestAnalysisTime());
            }
        } catch (Exception e) {
            LogUtils.error(">>> [DB Error] 保存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * Webhook 调用入口 - 更新指定需求的缺陷数量
     */
    public void updateStoryDefectCount(String projectKey, String storyId, String userEmail) {
        LogUtils.info("Webhook 收到更新请求，触发同步: storyId={}", storyId);
        syncStories(userEmail);
    }

    // ==================== 缺陷 CRUD（飞书双向同步） ====================

    public String getDefaultProjectKey() {
        return defaultProjectKey;
    }

    public String getDefectTypeKey() {
        return defectTypeKey;
    }

    /**
     * 获取创建工作项元数据（飞书 Open API：GET open_api/:project_key/work_item/:work_item_type_key/meta）
     * Postman：开放能力open-api接口 → 工作项 → 获取创建工作项元数据。
     * 返回的 data 中含各字段的 value_type 等，用于按「字段与属性解析格式」序列化 field_value：https://project.feishu.cn/b/helpcenter/1p8d7djs/1tj6ggll
     */
    public JsonNode getWorkItemCreateMeta(String projectKey, String userEmail) {
        if (StringUtils.isBlank(projectKey)) {
            return null;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/meta";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    return root.path("data");
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 获取创建工作项元数据失败: projectKey={}, e={}", projectKey, e.getMessage());
        }
        return null;
    }

    /**
     * 获取飞书流程状态选项列表（供表头筛选、详情状态下拉等使用）。
     * 从流程模板实时拉取，保证与飞书项目配置一致；value 与 text 均为展示名，同步到飞书时通过 getWorkflowStateKey 转为 state_key。
     *
     * @return 选项列表，每项含 "value"、"text"（均为流程状态展示名）；拉取失败或为空时返回空列表
     */
    public List<Map<String, String>> getWorkflowStatusOptions(String projectKey, String userEmail) {
        Map<String, String> nameToKey = getWorkflowStateNameToKeyMap(projectKey, userEmail);
        if (nameToKey == null || nameToKey.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> options = new ArrayList<>();
        for (String name : nameToKey.keySet()) {
            Map<String, String> item = new HashMap<>();
            item.put("value", name);
            item.put("text", name);
            options.add(item);
        }
        return options;
    }

    /**
     * 获取流程状态展示名→state_key 映射（用于 work_item_status 更新）。
     * 状态为「系统计算字段」，选项来自流程配置，需从「获取工作项下的流程模板列表」+「获取流程模板配置详情」拉取 workflow_confs。
     * Postman：流程配置 → 获取工作项下的流程模板列表、获取流程模板配置详情。
     */
    public String getWorkflowStateKey(String projectKey, String userEmail, String displayNameOrStateKey) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(displayNameOrStateKey)) {
            return displayNameOrStateKey;
        }
        Map<String, String> nameToKey = getWorkflowStateNameToKeyMap(projectKey, userEmail);
        if (nameToKey == null || nameToKey.isEmpty()) {
            return displayNameOrStateKey;
        }
        String trimmed = displayNameOrStateKey.trim();
        String byName = nameToKey.get(trimmed);
        if (StringUtils.isNotBlank(byName)) {
            return byName;
        }
        if (nameToKey.containsValue(trimmed)) {
            return trimmed;
        }
        return displayNameOrStateKey;
    }

    /**
     * 根据 state_key 反查流程状态展示名（name），用于将飞书状态代码还原为中文/英文展示值。
     * 入参既可以是 state_key 也可以是展示名；若已是展示名则原样返回。
     */
    public String getWorkflowStateDisplayName(String projectKey, String userEmail, String stateKeyOrName) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(stateKeyOrName)) {
            return stateKeyOrName;
        }
        Map<String, String> nameToKey = getWorkflowStateNameToKeyMap(projectKey, userEmail);
        if (nameToKey == null || nameToKey.isEmpty()) {
            return stateKeyOrName;
        }
        String trimmed = stateKeyOrName.trim();
        if (nameToKey.containsKey(trimmed)) {
            return trimmed;
        }
        for (Map.Entry<String, String> e : nameToKey.entrySet()) {
            if (trimmed.equals(e.getValue())) {
                return e.getKey();
            }
        }
        return stateKeyOrName;
    }

    /**
     * 拉取并缓存流程状态 name→state_key。缓存 5 分钟。
     */
    private Map<String, String> getWorkflowStateNameToKeyMap(String projectKey, String userEmail) {
        long now = System.currentTimeMillis();
        if (workflowStateNameToKeyCache != null && projectKey.equals(workflowStateCacheProjectKey) && now < workflowStateCacheExpire) {
            return workflowStateNameToKeyCache;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        try {
            String listUrl = baseUrl + "/open_api/" + projectKey + "/template_list/" + defectTypeKey;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);
            ResponseEntity<String> listResp = restTemplate.exchange(listUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (listResp.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(listResp.getBody())) {
                return null;
            }
            JsonNode listRoot = objectMapper.readTree(listResp.getBody());
            if (getResponseCode(listRoot) != 0) {
                LogUtils.warn("[Feishu] 获取流程模板列表失败: projectKey={}, err={}", projectKey, getErrorMsg(listRoot));
                return null;
            }
            JsonNode data = listRoot.path("data");
            JsonNode templates = data.isArray() ? data : (data.has("templates") ? data.path("templates") : data.path("list"));
            if (!templates.isArray() || templates.size() == 0) {
                LogUtils.info("[Feishu] 流程模板列表为空: projectKey={}", projectKey);
                return null;
            }
            JsonNode first = templates.get(0);
            long templateId = first.path("id").asLong(0);
            if (templateId == 0) {
                templateId = first.path("template_id").asLong(0);
            }
            if (templateId == 0) {
                return null;
            }
            String detailUrl = baseUrl + "/open_api/" + projectKey + "/template_detail/" + templateId;
            ResponseEntity<String> detailResp = restTemplate.exchange(detailUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (detailResp.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(detailResp.getBody())) {
                return null;
            }
            JsonNode detailRoot = objectMapper.readTree(detailResp.getBody());
            if (getResponseCode(detailRoot) != 0) {
                LogUtils.warn(String.format("[Feishu] 获取流程模板详情失败: projectKey=%s, templateId=%s, err=%s", projectKey, templateId, getErrorMsg(detailRoot)));
                return null;
            }
            JsonNode detailData = detailRoot.path("data");
            JsonNode workflowConfs = detailData.path("workflow_confs");
            if (!workflowConfs.isArray()) {
                return null;
            }
            Map<String, String> nameToKey = new HashMap<>();
            for (JsonNode conf : workflowConfs) {
                String stateKey = conf.has("state_key") ? conf.get("state_key").asText("").trim() : "";
                String name = conf.has("name") ? conf.get("name").asText("").trim() : "";
                if (StringUtils.isNotBlank(stateKey) && StringUtils.isNotBlank(name)) {
                    nameToKey.put(name, stateKey);
                }
            }
            if (!nameToKey.isEmpty()) {
                workflowStateNameToKeyCache = nameToKey;
                workflowStateCacheProjectKey = projectKey;
                workflowStateCacheExpire = now + 5 * 60 * 1000;
                LogUtils.info("[Feishu] 流程状态已缓存: projectKey={}, states={}", projectKey, nameToKey.keySet());
            }
            return nameToKey;
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 获取流程状态失败: projectKey={}, e={}", projectKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取缺陷类型下所有字段信息（飞书 Open API：POST open_api/:project_key/field/all）
     * 文档：https://project.feishu.cn/b/helpcenter/1p8d7djs/3tsposa2
     *
     * @param projectKey 空间 project_key 或 simple_name
     * @param userEmail  调用用户邮箱（可为空）
     * @return data 数组，每项含 key、name、type 等；失败返回空列表
     */
    public List<JsonNode> getDefectFieldList(String projectKey, String userEmail) {
        if (StringUtils.isBlank(projectKey)) {
            return Collections.emptyList();
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/field/all";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("work_item_type_key", defectTypeKey);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        List<JsonNode> list = new ArrayList<>();
                        data.forEach(list::add);
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 获取缺陷字段列表失败: projectKey={}, e={}", projectKey, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 获取指定字段在飞书缺陷类型下的全部枚举/选项（如缺陷原因 field_e84b00、优先级 priority 等）。
     * 通过飞书 Open API：POST open_api/:project_key/field/all，请求体 work_item_type_key=缺陷类型，返回的 data 中对应 field_key 的 options。
     *
     * @param projectKey 空间 project_key 或 simple_name
     * @param userEmail  调用用户邮箱（可为空）
     * @param fieldKey   字段 key，如 field_e84b00（缺陷原因）、priority、work_item_status
     * @return 该字段的 options 数组（保留树形结构，每项含 label、value/state_key、children 等）；无此字段或无可选项时返回空列表
     */
    public List<JsonNode> getDefectFieldOptions(String projectKey, String userEmail, String fieldKey) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(fieldKey)) {
            return Collections.emptyList();
        }
        List<JsonNode> fields = getDefectFieldList(projectKey, userEmail);
        String fk = fieldKey.trim();
        for (JsonNode f : fields) {
            String key = f.has("field_key") ? f.get("field_key").asText("") : f.path("key").asText("");
            if (!fk.equals(key.trim())) continue;
            if (!f.has("options") || !f.get("options").isArray()) return Collections.emptyList();
            List<JsonNode> list = new ArrayList<>();
            f.get("options").forEach(list::add);
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * 获取当前空间缺陷类型下可用的字段 key 集合（用于 create 时只传飞书支持的字段，避免 30009）
     * 结果缓存 5 分钟。
     */
    private Set<String> getAllowedDefectFieldKeys(String projectKey, String userEmail) {
        long now = System.currentTimeMillis();
        if (defectFieldKeysCache != null && projectKey != null && projectKey.equals(defectFieldKeysProjectKey) && now < defectFieldKeysExpire) {
            return defectFieldKeysCache;
        }
        List<JsonNode> fields = getDefectFieldList(projectKey, userEmail);
        Set<String> keys = new HashSet<>();
        Map<String, String> typeMap = new HashMap<>();
        Map<String, String> priorityOptions = new HashMap<>();
        Map<String, String> defectReasonOptions = new HashMap<>();
        Map<String, Map<String, String>> allFieldOptions = new HashMap<>();
        for (JsonNode f : fields) {
            String fk = f.has("field_key") ? f.get("field_key").asText("") : f.path("key").asText("");
            if (StringUtils.isBlank(fk)) continue;
            fk = fk.trim();
            keys.add(fk);
            String typeKey = f.has("field_type_key") ? f.get("field_type_key").asText("") : f.path("type").asText("");
            if (StringUtils.isNotBlank(typeKey)) {
                typeMap.put(fk, typeKey.trim());
            }
            if (f.has("options") && f.get("options").isArray()) {
                Map<String, String> opts = new HashMap<>();
                collectOptionLabelToValue(f.get("options"), opts);
                if (!opts.isEmpty()) {
                    allFieldOptions.put(fk, opts);
                    if ("priority".equals(fk)) {
                        priorityOptions.putAll(opts);
                    }
                    if ("field_e84b00".equals(fk)) {
                        defectReasonOptions.putAll(opts);
                    }
                }
            }
        }
        if (!keys.isEmpty()) {
            defectFieldKeysProjectKey = projectKey;
            defectFieldKeysExpire = now + 5 * 60 * 1000;
            defectFieldKeysCache = keys;
            defectFieldTypeCache = typeMap;
            // 用「获取创建工作项元数据」补充 value_type，严格按 字段与属性解析格式 序列化 field_value
            Map<String, Integer> valueTypeMap = new HashMap<>();
            JsonNode meta = getWorkItemCreateMeta(projectKey, userEmail);
            if (meta != null) {
                JsonNode fieldsNode = meta.isArray() ? meta : meta.path("fields");
                if (fieldsNode.isArray()) {
                    for (JsonNode f : fieldsNode) {
                        String fk = f.has("field_key") ? f.get("field_key").asText("") : "";
                        if (StringUtils.isBlank(fk)) continue;
                        if (f.has("value_type")) {
                            valueTypeMap.put(fk.trim(), f.get("value_type").asInt(-1));
                        }
                    }
                }
                if (!valueTypeMap.isEmpty()) {
                    defectFieldValueTypeCache = valueTypeMap;
                }
            }
            if (!priorityOptions.isEmpty()) {
                priorityLabelToOptionValueCache = priorityOptions;
                LogUtils.info("[Feishu] 优先级选项已缓存: projectKey={}, labels={}", projectKey, priorityOptions.keySet());
            }
            if (!defectReasonOptions.isEmpty()) {
                defectReasonLabelToOptionValueCache = defectReasonOptions;
                LogUtils.info("[Feishu] 缺陷原因选项已缓存: projectKey={}, labels={}", projectKey, defectReasonOptions.keySet());
            }
            if (!allFieldOptions.isEmpty()) {
                fieldOptionLabelToValueCaches = allFieldOptions;
                LogUtils.info("[Feishu] 选项字段已统一缓存: projectKey={}, fieldKeys={}", projectKey, allFieldOptions.keySet());
            }
            LogUtils.info("[Feishu] 缺陷字段列表已刷新: projectKey={}, count={}, types={}, valueTypes={}", projectKey, keys.size(), typeMap.size(), valueTypeMap.size());
        }
        return keys;
    }

    /** 递归收集 options（含 children）中的 label→value，用于 tree_select 等；支持 value 或 state_key 作为 value */
    private void collectOptionLabelToValue(JsonNode options, Map<String, String> out) {
        if (options == null || !options.isArray()) return;
        for (JsonNode opt : options) {
            String label = opt.has("label") ? opt.get("label").asText("").trim() : "";
            String value = opt.has("value") ? opt.get("value").asText("").trim() : (opt.has("state_key") ? opt.get("state_key").asText("").trim() : "");
            if (StringUtils.isNotBlank(label) && StringUtils.isNotBlank(value)) {
                out.put(label, value);
            }
            if (opt.has("children") && opt.get("children").isArray()) {
                collectOptionLabelToValue(opt.get("children"), out);
            }
        }
    }

    /**
     * 按当前空间接口返回的选项，将任意字段的展示值解析为飞书 option/state value，避免 20050 等选项过期。
     * 若该 fieldKey 有缓存且 displayValue 匹配到 label 或已是有效 value，返回 value；否则返回 null。
     */
    public String getOptionValue(String projectKey, String userEmail, String fieldKey, String displayValue) {
        if (StringUtils.isBlank(fieldKey) || StringUtils.isBlank(displayValue)) return null;
        getAllowedDefectFieldKeys(projectKey, userEmail);
        if (fieldOptionLabelToValueCaches == null) return null;
        Map<String, String> m = fieldOptionLabelToValueCaches.get(fieldKey);
        if (m == null) return null;
        String trimmed = displayValue.trim();
        String byLabel = m.get(trimmed);
        if (StringUtils.isNotBlank(byLabel)) return byLabel;
        if (m.containsValue(trimmed)) return trimmed;
        return null;
    }

    /**
     * 按当前空间「优先级」字段的选项，将展示值（P0/P1/...）解析为飞书 option value，避免 20050 option value expired。
     * 若空间未配置或无对应选项则返回 null，调用方可不传 priority。
     */
    public String getPriorityOptionValue(String projectKey, String userEmail, String priorityDisplay) {
        if (StringUtils.isBlank(priorityDisplay)) return null;
        getAllowedDefectFieldKeys(projectKey, userEmail);
        if (priorityLabelToOptionValueCache == null) return null;
        String value = priorityLabelToOptionValueCache.get(priorityDisplay.trim());
        return StringUtils.isNotBlank(value) ? value : null;
    }

    /**
     * 按当前空间「缺陷原因」字段(field_e84b00)的选项，将展示值解析为飞书 option value，避免 20050 option value expired。
     * 与 Webhook 解析一致：飞书 tree_select 在 Webhook 里被解析为 "一级" 或 "一级_二级"（label 路径），
     * 此处先精确匹配，再尝试用最后一段（叶子 label）匹配，以便 "漏测_用例未设计" → 叶子选项 value；若已是 option value 则原样返回。
     */
    public String getDefectReasonOptionValue(String projectKey, String userEmail, String defectReasonDisplay) {
        if (StringUtils.isBlank(defectReasonDisplay)) return null;
        getAllowedDefectFieldKeys(projectKey, userEmail);
        if (defectReasonLabelToOptionValueCache == null) return null;
        String trimmed = defectReasonDisplay.trim();
        String value = defectReasonLabelToOptionValueCache.get(trimmed);
        if (StringUtils.isNotBlank(value)) return value;
        if (defectReasonLabelToOptionValueCache.containsValue(trimmed)) return trimmed;
        int lastUnderscore = trimmed.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < trimmed.length() - 1) {
            String leafLabel = trimmed.substring(lastUnderscore + 1).trim();
            value = defectReasonLabelToOptionValueCache.get(leafLabel);
            if (StringUtils.isNotBlank(value)) return value;
        }
        return null;
    }

    /**
     * 按飞书官方「字段与属性解析格式」将 field_value 序列化为创建工作项入参要求。
     * 文档：https://project.feishu.cn/b/helpcenter/1p8d7djs/1tj6ggll ；请求体结构见 Postman「创建工作项」。
     * 优先使用 meta 返回的 value_type，否则按 field_type_key。
     */
    private Object formatFieldValueForCreate(String fieldKey, Object value, String fieldTypeKey) {
        if (value == null) return null;
        if (value instanceof Map) return value;
        if (value instanceof List) return value; // multi_user(watchers)、role_owners 等已是 List，直接透传
        if (value instanceof Number) {
            // 关联需求（field_a62d41 / work_item_related_*）：需求ID 为工作项实例ID，必须为数字类型，且需通过「获取工作项列表」确保 ID 存在于当前空间，否则 30005 WorkItem Not Found
            if ("field_a62d41".equals(fieldKey) || "_field_linked_story".equals(fieldKey) || "related_requirements".equals(fieldKey)
                    || (fieldKey != null && fieldKey.startsWith("work_item_related_"))) {
                return ((Number) value).longValue();
            }
            return value;
        }
        String str = value.toString().trim();
        if (StringUtils.isBlank(str)) return value;

        Integer valueType = (defectFieldValueTypeCache != null) ? defectFieldValueTypeCache.get(fieldKey) : null;
        if (valueType != null && valueType >= 0) {
            // 按 value_type 严格遵循 字段与属性解析格式
            switch (valueType) {
                case 0:
                    return str; // string
                case 1:
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException e) {
                        try {
                            return Double.parseDouble(str);
                        } catch (NumberFormatException e2) {
                            return value;
                        }
                    }
                case 2:
                    return Map.of("value", str); // 单选 option
                case 3:
                    return str; // 人员 user_key
                case 4:
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException e) {
                        return value;
                    }
                default:
                    break;
            }
        }

        if (StringUtils.isNotBlank(fieldTypeKey)) {
            switch (fieldTypeKey) {
                case "select":
                case "single_select":
                    return Map.of("value", str);
                case "multi_select":
                    return List.of(Map.of("value", str));
                case "user":
                case "single_user":
                    return str;
                case "multi_user":
                    return List.of(str);
                case "number":
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException e) {
                        try {
                            return Double.parseDouble(str);
                        } catch (NumberFormatException e2) {
                            return value;
                        }
                    }
                case "tree_select":
                case "cascading":
                    return Map.of("value", str);
                case "text":
                case "multi_text":
                default:
                    return value;
            }
        }
        if ("priority".equals(fieldKey) || "business".equals(fieldKey)) {
            return Map.of("value", str);
        }
        return value;
    }

    /**
     * 上传文件到飞书项目（用于富文本描述中的图片），走 Open API：POST /open_api/:project_key/file/upload
     *
     * @param projectKey 项目 key
     * @param userEmail  调用用户邮箱（可为空，用默认）
     * @param fileBytes  文件内容
     * @param fileName   文件名（用于 Content-Disposition）
     * @return 飞书返回的 uuid，用于 doc_rich_text.doc_img
     */
    public String uploadFile(String projectKey, String userEmail, byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0 || StringUtils.isBlank(fileName)) {
            return null;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/file/upload";

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    String uuid = data.has("uuid") ? data.path("uuid").asText() : (data.has("file_id") ? data.path("file_id").asText() : null);
                    if (StringUtils.isNotBlank(uuid)) {
                        LogUtils.info("[Feishu] 文件上传成功: projectKey={}, fileName={}, uuid={}", projectKey, fileName, uuid);
                        return uuid;
                    }
                }
                throw new MSException("飞书文件上传失败: " + getErrorMsg(root));
            }
            throw new MSException("飞书文件上传HTTP异常: " + resp.getStatusCode());
        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            throw new MSException("飞书文件上传异常: " + e.getMessage());
        }
    }

    /**
     * 创建飞书缺陷工作项。
     * 请求体与 field_value 格式严格按：Postman「创建工作项」+ 字段与属性解析格式 https://project.feishu.cn/b/helpcenter/1p8d7djs/1tj6ggll ；详见 docs/feishu-work-item-create-format.md。
     * descriptionValue 可为 String（纯文本）或 Map（doc_rich_text：含 doc_html、doc_text、doc_img、is_empty 等），为 null 或空则不传描述。
     *
     * @return 飞书 work_item_id
     */
    public String createDefect(String projectKey, String userEmail, String name,
                                Object descriptionValue, Map<String, Object> extraFieldValues) {
        LogUtils.info("[Feishu] createDefect 入口: projectKey={}, name={}, descPresent={}", projectKey, name, descriptionValue != null);
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/create";

        try {
            // 使用飞书「获取字段信息」接口返回的缺陷字段 key，只传飞书实际支持的字段，避免 30009
            Set<String> allowedExtraKeys = getAllowedDefectFieldKeys(projectKey, resolveUserEmail(userEmail));
            if (allowedExtraKeys.isEmpty()) {
                // 与飞书「获取字段信息」返回的 field_key 一致：owner、field_e84b00 等
                allowedExtraKeys = Set.of(
                        "business", "priority", "owner", "field_e84b00",
                        "field_1cbc4e", "field_39dbe4", "field_f12022"
                );
            }
            // 创建时系统字段 template_id 不通过 field_value_pairs 传，始终允许；关注人、角色与人员部分空间用语义 key
            // 关联需求 field_a62d41 仅当该空间缺陷类型包含该字段时才传（由 allowedExtraKeys 决定），避免 20006 field is illegal
            Set<String> allowTemplate = new HashSet<>(allowedExtraKeys);
            allowTemplate.add("template_id");
            allowTemplate.add("watchers");
            allowTemplate.add("role_owners");

            Map<String, Object> body = new HashMap<>();
            // 与 OpenAPI 一致：field_value_pairs 每项可含 field_key、field_value、target_state（状态用 target_state.state_key）
            List<Map<String, Object>> fieldValuePairs = new ArrayList<>();
            if (descriptionValue != null) {
                boolean hasContent = descriptionValue instanceof String
                    ? StringUtils.isNotBlank((String) descriptionValue)
                    : (descriptionValue instanceof Map && !((Map<?, ?>) descriptionValue).isEmpty());
                if (hasContent) {
                    Map<String, Object> descField = new HashMap<>();
                    descField.put("field_key", "description");
                    descField.put("field_value", descriptionValue);
                    if (defectFieldTypeCache != null && defectFieldTypeCache.containsKey("description")) {
                        descField.put("field_type_key", defectFieldTypeCache.get("description"));
                    }
                    fieldValuePairs.add(descField);
                }
            }
            if (extraFieldValues != null) {
                extraFieldValues.forEach((key, value) -> {
                    if (StringUtils.isBlank(key)) {
                        // 对接标识未配置或为空的字段不传，避免 30009 Field Not Found
                        return;
                    }
                    // 只传飞书该空间缺陷类型下存在的字段 key，避免 30009
                    if (!allowTemplate.contains(key)) {
                        return;
                    }
                    // 状态在创建时用 target_state 单独语义，且部分空间创建接口不接受，此处不传
                    if ("work_item_status".equals(key)) {
                        return;
                    }
                    Map<String, Object> pair = new HashMap<>();
                    if ("template_id".equals(key)) {
                        // 缺陷类型是系统字段，按 OpenAPI 顶层 template_id 传，不走 field_value_pairs
                        if (value != null && StringUtils.isNotBlank(value.toString())) {
                            try {
                                long templateId = Long.parseLong(value.toString());
                                body.put("template_id", templateId);
                            } catch (NumberFormatException ignore) {
                                body.put("template_id", value.toString());
                            }
                        }
                    } else {
                        pair.put("field_key", key);
                        String typeKey = (defectFieldTypeCache != null) ? defectFieldTypeCache.get(key) : null;
                        Object fieldValue = formatFieldValueForCreate(key, value, typeKey);
                        pair.put("field_value", fieldValue);
                        if (StringUtils.isNotBlank(typeKey)) {
                            pair.put("field_type_key", typeKey);
                        }
                    }
                    if (!pair.isEmpty()) {
                        fieldValuePairs.add(pair);
                    }
                });
            }

            body.put("work_item_type_key", defectTypeKey);
            body.put("name", name);
            if (!fieldValuePairs.isEmpty()) {
                body.put("field_value_pairs", fieldValuePairs);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode dataNode = root.path("data");
                    String workItemId = null;
                    if (dataNode.isNumber() || dataNode.isTextual()) {
                        workItemId = dataNode.asText();
                    } else if (dataNode.isObject()) {
                        if (dataNode.has("id")) {
                            workItemId = dataNode.get("id").asText();
                        }
                        if (StringUtils.isBlank(workItemId) && dataNode.has("work_item_id")) {
                            workItemId = dataNode.get("work_item_id").asText();
                        }
                    }
                    if (StringUtils.isNotBlank(workItemId)) {
                        LogUtils.info("[Feishu] 创建缺陷成功: projectKey={}, workItemId={}", projectKey, workItemId);
                        return workItemId;
                    }
                }
                throw new MSException("飞书创建缺陷失败: " + getErrorMsg(root));
            }
            throw new MSException("飞书创建缺陷HTTP异常: " + resp.getStatusCode());
        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            throw new MSException("飞书创建缺陷异常: " + e.getMessage());
        }
    }

    /**
     * 获取从当前节点到目标状态所需的流转信息（含 transition_id）。
     * 推荐按「获取工作流详情」文档：https://project.feishu.cn/b/helpcenter/2.0.0/1p8d7djs/5hcmcl04
     * 基于 work_item_id 拉取 workflow/task，再从返回的 connections 中找到 to_state_key 对应的 transition_id。
     *
     * @return 可用的 transition_id，若无法获取则返回 null
     */
    private Integer getTransitionRequiredInfo(String projectKey, String userEmail, String workItemId, String targetStateKey) {
        if (StringUtils.isBlank(workItemId) || StringUtils.isBlank(targetStateKey)) {
            return null;
        }
        long workItemIdLong;
        try {
            workItemIdLong = Long.parseLong(workItemId.trim());
        } catch (NumberFormatException e) {
            LogUtils.warn("[Feishu] getTransitionRequiredInfo work_item_id 非数字: workItemId={}", workItemId);
            return null;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        // 使用「查询状态流工作项所有信息」接口：POST /open_api/:project_key/work_item/:work_item_type_key/:work_item_id/workflow/query
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/" + workItemIdLong + "/workflow/query";

        try {
            Map<String, Object> body = new HashMap<>();
            // 按官方示例：flow_type=1，带上 need_user_detail
            body.put("flow_type", 1);
            Map<String, Object> expand = new HashMap<>();
            expand.put("need_user_detail", true);
            body.put("expand", expand);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(resp.getBody())) {
                LogUtils.warn("[Feishu] getTransitionRequiredInfo 请求异常: workItemId={}, status={}", workItemId, resp.getStatusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (getResponseCode(root) != 0) {
                LogUtils.warn(String.format("[Feishu] getTransitionRequiredInfo 接口返回错误: workItemId=%s, state_key=%s, err=%s",
                        workItemId, targetStateKey, getErrorMsg(root)));
                return null;
            }
            JsonNode data = root.path("data");

            // 1. 找到当前状态：state_flow_nodes 中 status == 2 的节点，其 id 即为当前 state_key
            // 同时构建 name → id 映射，用于将展示名（如"已解决"）解析为实际 state_key（如"fYFKOOeAM"）
            String currentStateKey = null;
            Map<String, String> nodeNameToId = new HashMap<>();
            JsonNode stateFlowNodes = data.path("state_flow_nodes");
            LogUtils.info(String.format("[Feishu] getTransitionRequiredInfo 返回 state_flow_nodes: workItemId=%s, hasNodes=%s",
                    workItemId, stateFlowNodes.isArray()));
            if (stateFlowNodes.isArray()) {
                for (JsonNode node : stateFlowNodes) {
                    String nodeId = node.path("id").asText("");
                    String nodeName = node.path("name").asText("");
                    if (StringUtils.isNotBlank(nodeId) && StringUtils.isNotBlank(nodeName)) {
                        nodeNameToId.put(nodeName, nodeId);
                    }
                    if (node.has("status") && node.get("status").asInt() == 2) {
                        currentStateKey = nodeId;
                    }
                }
            }
            if (StringUtils.isBlank(currentStateKey)) {
                // 若未找到当前节点，则无法精确匹配 source_state_key，只能记录日志并返回 null
                LogUtils.warn(String.format("[Feishu] getTransitionRequiredInfo 未找到当前状态节点(state_flow_nodes.status=2): workItemId=%s", workItemId));
                return null;
            }

            // targetStateKey 可能是展示名（如"已解决"）而非实际 state_key（如"fYFKOOeAM"），
            // 用 state_flow_nodes 的 name→id 映射做一次兜底解析
            String resolvedTargetStateKey = targetStateKey;
            if (nodeNameToId.containsKey(targetStateKey)) {
                resolvedTargetStateKey = nodeNameToId.get(targetStateKey);
                LogUtils.info(String.format("[Feishu] getTransitionRequiredInfo 展示名→state_key: '%s' → '%s'",
                        targetStateKey, resolvedTargetStateKey));
            } else if (!nodeNameToId.containsValue(targetStateKey)) {
                // targetStateKey 既不是 name 也不是已知的 id，打印所有节点供排查
                LogUtils.warn(String.format("[Feishu] getTransitionRequiredInfo targetStateKey '%s' 不在 state_flow_nodes 中, 可用节点: %s",
                        targetStateKey, nodeNameToId));
            }

            LogUtils.info(String.format("[Feishu] getTransitionRequiredInfo 当前状态: workItemId=%s, currentStateKey=%s, targetStateKey=%s, resolvedTargetStateKey=%s",
                    workItemId, currentStateKey, targetStateKey, resolvedTargetStateKey));

            // 2. 在 connections 中查找从 currentStateKey 流转到 resolvedTargetStateKey 的那条边，取其 transition_id
            JsonNode connections = data.path("connections");
            if (connections.isArray()) {
                for (JsonNode c : connections) {
                    String source = c.path("source_state_key").asText();
                    String target = c.path("target_state_key").asText();
                    if (currentStateKey.equals(source) && resolvedTargetStateKey.equals(target) && c.has("transition_id")) {
                        LogUtils.info(String.format("[Feishu] getTransitionRequiredInfo 命中流转: workItemId=%s, source=%s, target=%s, transitionId=%s",
                                workItemId, source, target, c.get("transition_id").asInt()));
                        return c.get("transition_id").asInt();
                    }
                }
            }

            LogUtils.warn(String.format("[Feishu] getTransitionRequiredInfo 响应中未解析到 transition_id（请检查 workflow.query 返回的 connections）: workItemId=%s, current=%s, target=%s (resolved=%s)",
                    workItemId, currentStateKey, targetStateKey, resolvedTargetStateKey));
            return null;
        } catch (Exception e) {
            LogUtils.warn(String.format("[Feishu] getTransitionRequiredInfo 异常: workItemId=%s, state_key=%s, error=%s",
                    workItemId, targetStateKey, e.getMessage()));
            return null;
        }
    }

    /**
     * 获取缺陷从当前状态可流转的目标状态选项
     * 仅返回当前用户拥有 operator 权限的目标状态
     */
    public List<io.vanguard.testops.plugin.platform.dto.SelectOption> getValidStatusTransitions(String projectKey, String userEmail, String workItemId) {
        if (StringUtils.isBlank(workItemId)) {
            return Collections.emptyList();
        }
        long workItemIdLong;
        try {
            workItemIdLong = Long.parseLong(workItemId.trim());
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }

        String token = getPluginToken();
        String resolvedEmail = resolveUserEmail(userEmail);
        String userKeyStr = getUserKeyByEmail(resolvedEmail);
        if (StringUtils.isBlank(userKeyStr)) {
            LogUtils.warn(String.format(
                    "[Feishu] getValidStatusTransitions userKey 为空, projectKey=%s, email=%s, workItemId=%s",
                    projectKey, resolvedEmail, workItemId));
            return Collections.emptyList();
        }

        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/" + workItemIdLong + "/workflow/query";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("flow_type", 1);
            Map<String, Object> expand = new HashMap<>();
            expand.put("need_user_detail", true);
            body.put("expand", expand);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKeyStr);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(resp.getBody())) {
                LogUtils.warn(String.format(
                        "[Feishu] getValidStatusTransitions HTTP 异常, status=%s, body=%s",
                        resp.getStatusCode(), resp.getBody()));
                return Collections.emptyList();
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (getResponseCode(root) != 0) {
                LogUtils.warn(String.format(
                        "[Feishu] getValidStatusTransitions 返回 errCode!=0, body=%s",
                        resp.getBody()));
                return Collections.emptyList();
            }
            JsonNode data = root.path("data");

            // 找到当前状态 (status=2 的节点) 的 id (state_key)
            String currentStateKey = null;
            JsonNode stateFlowNodes = data.path("state_flow_nodes");
            Map<String, JsonNode> nodeMap = new HashMap<>();
            if (stateFlowNodes.isArray()) {
                for (JsonNode node : stateFlowNodes) {
                    nodeMap.put(node.path("id").asText(), node);
                    if (node.has("status") && node.get("status").asInt() == 2) {
                        currentStateKey = node.path("id").asText();
                    }
                }
            }
            if (currentStateKey == null) {
                return Collections.emptyList();
            }

            // 从 connections 找到允许的目标 state_key
            Set<String> targetStateKeys = new HashSet<>();
            JsonNode connections = data.path("connections");
            if (connections.isArray()) {
                for (JsonNode c : connections) {
                    if (currentStateKey.equals(c.path("source_state_key").asText())) {
                        targetStateKeys.add(c.path("target_state_key").asText());
                    }
                }
            }

            // 结合 connections + role_owners 计算当前用户可见的目标状态：
            // 1）先按 connections 过滤出当前状态可达的目标节点；
            // 2）若目标节点在 role_owners 中对 reporter/operator 做了 owner 限制，则仅当当前 userKey 在 owners 中时才返回；
            // 3）若没有对 reporter/operator 做 owner 限制（owners 为空或未配置），则认为该节点对当前用户开放。
            List<io.vanguard.testops.plugin.platform.dto.SelectOption> options = new ArrayList<>();
            for (String tsKey : targetStateKeys) {
                JsonNode tsNode = nodeMap.get(tsKey);
                if (tsNode == null) {
                    continue;
                }

                boolean hasRoleConstraint = false;
                boolean matchedRoleOwner = false;
                JsonNode roles = tsNode.path("role_owners");
                if (roles.isArray()) {
                    for (JsonNode role : roles) {
                        String roleName = role.path("role").asText();
                        // 只关注与我们系统映射的 reporter/operator 角色
                        if (!"reporter".equals(roleName) && !"operator".equals(roleName)) {
                            continue;
                        }
                        JsonNode owners = role.path("owners");
                        // owners 为空或不存在时，视为该角色对所有人开放，不参与权限限制
                        if (owners == null || !owners.isArray() || owners.isEmpty()) {
                            continue;
                        }
                        hasRoleConstraint = true;
                        for (JsonNode o : owners) {
                            if (userKeyStr.equals(o.asText())) {
                                matchedRoleOwner = true;
                                break;
                            }
                        }
                        if (matchedRoleOwner) {
                            break;
                        }
                    }
                }

                // 如果没有对 reporter/operator 做约束，则认为该状态对当前用户开放；
                // 若有约束，则当前用户必须在 owners 中才可见。
                boolean hasAccess = !hasRoleConstraint || matchedRoleOwner;
                if (!hasAccess) {
                    continue;
                }

                String tsName = tsNode.path("name").asText();
                if (StringUtils.isBlank(tsName)) {
                    continue;
                }
                io.vanguard.testops.plugin.platform.dto.SelectOption option = new io.vanguard.testops.plugin.platform.dto.SelectOption();
                option.setText(tsName);
                // BugService 使用展示名作为 value 传回
                option.setValue(tsName);
                options.add(option);
            }
            return options;
        } catch (Exception e) {
            LogUtils.error("[Feishu] getValidStatusTransitions failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 执行状态流转。文档：https://project.feishu.cn/b/helpcenter/2.0.0/1p8d7djs/4xve8n8c
     * 接口：POST open_api/:project_key/workflow/:work_item_type_key/:work_item_id/node/state_change
     */
    private void executeStateChange(String projectKey, String userEmail, String workItemId, int transitionId,
                                    List<Map<String, Object>> fields, List<Map<String, Object>> roleOwners) {
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/workflow/" + defectTypeKey + "/" + workItemId + "/node/state_change";

        Map<String, Object> body = new HashMap<>();
        body.put("transition_id", transitionId);
        body.put("fields", fields != null ? fields : Collections.emptyList());
        // 注意：role_owners 为 null 时不下发该字段，避免清空飞书侧已有的报告人/经办人配置
        if (roleOwners != null) {
            body.put("role_owners", roleOwners);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-PLUGIN-TOKEN", token);
        headers.set("X-USER-KEY", userKey);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (resp.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(resp.getBody())) {
            throw new MSException("飞书状态流转HTTP异常: " + (resp.getBody() != null ? resp.getBody() : resp.getStatusCode()));
        }
        try {
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (getResponseCode(root) != 0) {
                throw new MSException("飞书状态流转失败: " + getErrorMsg(root));
            }
            LogUtils.info("[Feishu] 状态流转成功: workItemId={}, transitionId={}", workItemId, transitionId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new MSException("飞书状态流转响应解析异常: " + e.getMessage());
        }
    }

    /**
     * 更新飞书缺陷工作项。
     * 严格按开放平台「更新工作项」规范：先拉取 meta（通过 getAllowedDefectFieldKeys 刷新字段与 field_type_key），
     * 请求体仅使用 update_fields，每项含 field_key、field_value 或 target_state、field_type_key（来自 meta/字段信息）。
     * 状态变更通过「状态流转」接口执行（见 https://project.feishu.cn/b/helpcenter/2.0.0/1p8d7djs/4xve8n8c），不再走 update_fields 的 target_state。
     */
    public void updateDefect(String projectKey, String userEmail, String workItemId,
                              Map<String, Object> updateFieldMap) {
        LogUtils.info("[Feishu] updateDefect 入口: projectKey={}, workItemId={}, fields={}",
                projectKey, workItemId, updateFieldMap != null ? updateFieldMap.keySet() : "null");
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/" + workItemId;

        try {
            // 状态变更：使用「状态流转」接口，先拉取 transition_id 再调用 state_change
            if (updateFieldMap != null && updateFieldMap.containsKey("work_item_status")) {
                Object statusVal = updateFieldMap.get("work_item_status");
                String targetStateKey = statusVal == null ? null : statusVal.toString();
                if (StringUtils.isNotBlank(targetStateKey)) {
                    Integer transitionId = getTransitionRequiredInfo(projectKey, userEmail, workItemId, targetStateKey);
                    if (transitionId != null) {
                        // 仅执行状态流转，不再下发 role_owners，避免覆盖飞书已有的角色与人员配置（特别是 reporter）。
                        executeStateChange(projectKey, userEmail, workItemId, transitionId, Collections.emptyList(), null);
                    } else {
                        LogUtils.warn(String.format("[Feishu] updateDefect 未获取到状态流转 transition_id, 跳过状态更新: workItemId=%s, state_key=%s",
                                workItemId, targetStateKey));
                    }
                }
            }

            // 先拉 meta：刷新字段列表与 field_type_key 缓存，保证按格式传参
            Set<String> allowedKeys = getAllowedDefectFieldKeys(projectKey, userEmail);
            LogUtils.info(String.format(
                    "[Feishu] updateDefect 可用字段: projectKey=%s, allowedSize=%d",
                    projectKey, allowedKeys != null ? allowedKeys.size() : 0));

            // 请求体仅使用 update_fields（不含 work_item_status，状态已走 state_change）；name/description 也作为 update_fields 项传入
            List<Map<String, Object>> updateFields = new ArrayList<>();
            if (updateFieldMap != null && !updateFieldMap.isEmpty()) {
                for (Map.Entry<String, Object> e : updateFieldMap.entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    // 状态已通过状态流转接口更新，不再放入 update_fields
                    if ("work_item_status".equals(key)) {
                        continue;
                    }
                    // template_id 不可通过更新接口修改，跳过
                    if ("template_id".equals(key) || "template".equals(key)) {
                        continue;
                    }
                    Map<String, Object> item = buildUpdateFieldItem(key, value, allowedKeys);
                    if (item != null && !item.isEmpty()) {
                        LogUtils.info(String.format(
                                "[Feishu] updateDefect 字段组装: fieldKey=%s, hasTypeKey=%s, useTargetState=%s",
                                key,
                                item.containsKey("field_type_key"),
                                item.containsKey("target_state")));
                        item.put("update_mode", 0);
                        updateFields.add(item);
                    }
                }
            }

            if (updateFields.isEmpty()) {
                LogUtils.info("[Feishu] updateDefect: 无需更新字段, 直接返回 workItemId={}", workItemId);
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("update_fields", updateFields);
            try {
                String bodyJson = objectMapper.writeValueAsString(body);
                LogUtils.info("[Feishu] updateDefect 请求体: " + bodyJson);
            } catch (Exception ignore) {
                LogUtils.warn("[Feishu] updateDefect 请求体序列化失败, workItemId={}", workItemId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            restTemplate.put(url, new HttpEntity<>(body, headers));
            LogUtils.info("[Feishu] 更新缺陷成功: workItemId={}", workItemId);
        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            LogUtils.error("[Feishu] 更新缺陷失败: workItemId={}, error={}", workItemId, e.getMessage());
            throw new MSException("飞书更新缺陷异常: " + e.getMessage());
        }
    }

    /**
     * 构造单条 update_fields 项，含 field_key、field_value 或 target_state、field_type_key（来自 meta）。
     */
    private Map<String, Object> buildUpdateFieldItem(String fieldKey, Object value, Set<String> allowedKeys) {
        Map<String, Object> item = new HashMap<>();
        item.put("field_key", fieldKey);
        String typeKey = (defectFieldTypeCache != null) ? defectFieldTypeCache.get(fieldKey) : null;
        if (StringUtils.isNotBlank(typeKey)) {
            item.put("field_type_key", typeKey);
        }

        // work_item_status 不应出现在 update_fields 中——状态变更已由 updateDefect 通过 state_change 接口完成
        if ("work_item_status".equals(fieldKey)) {
            return null;
        }

        // name、description 为空时不传，避免把飞书上的名称/描述置空
        if ("name".equals(fieldKey) || "description".equals(fieldKey)) {
            if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                return null;
            }
        }
        // name、description 及其他普通字段：仅当在 allowedKeys 中或为系统字段 name/description 时传入
        if (!"name".equals(fieldKey) && !"description".equals(fieldKey)
                && (allowedKeys == null || !allowedKeys.contains(fieldKey))) {
            return null;
        }
        Object fieldValue = formatFieldValueForCreate(fieldKey, value, typeKey);
        item.put("field_value", fieldValue != null ? fieldValue : value);
        return item;
    }

    /**
     * 删除飞书缺陷工作项
     */
    public void deleteDefect(String projectKey, String userEmail, String workItemId) {
        LogUtils.info("[Feishu] deleteDefect 入口: projectKey={}, workItemId={}", projectKey, workItemId);
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/" + workItemId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE,
                    new HttpEntity<>(headers), String.class);
            LogUtils.info("[Feishu] 删除缺陷成功: workItemId={}", workItemId);
        } catch (Exception e) {
            LogUtils.error("[Feishu] 删除缺陷失败: workItemId={}, error={}", workItemId, e.getMessage());
            throw new MSException("飞书删除缺陷异常: " + e.getMessage());
        }
    }

    /**
     * 查询飞书缺陷详情（按 ID 列表）
     *
     * @return 工作项 JsonNode 列表
     */
    public List<JsonNode> getDefectDetails(String projectKey, String userEmail, List<Long> workItemIds) {
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + defectTypeKey + "/query";

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("work_item_ids", workItemIds);

            Map<String, Object> expand = new HashMap<>();
            expand.put("need_multi_text", true);
            expand.put("relation_fields_detail", true);
            expand.put("need_user_detail", true);
            body.put("expand", expand);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    List<JsonNode> results = new ArrayList<>();
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            results.add(item);
                        }
                    } else if (!data.isMissingNode() && data.isObject()) {
                        results.add(data);
                    }
                    return results;
                }
                throw new MSException("飞书查询缺陷详情失败: " + getErrorMsg(root));
            }
            throw new MSException("飞书查询缺陷详情HTTP异常: " + resp.getStatusCode());
        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            throw new MSException("飞书查询缺陷详情异常: " + e.getMessage());
        }
    }

    /**
     * 拉取飞书项目下所有缺陷（分页，用于迁移）
     */
    public List<JsonNode> fetchAllDefects(String projectKey, String userEmail) {
        return fetchWorkItemsByProject(getPluginToken(), getUserKeyByEmail(resolveUserEmail(userEmail)), projectKey, defectTypeKey);
    }

    /**
     * 查询工作项评论（飞书 Open API：查询评论）
     * 用于缺陷详情页拉取飞书侧评论，评论内容可能含富文本和图片（doc_rich_text.doc_img）。
     *
     * @param projectKey      项目 key
     * @param userEmail       调用用户邮箱（为空则用 default-user-email）
     * @param workItemTypeKey 工作项类型 key，缺陷传 getDefectTypeKey()
     * @param workItemId      工作项 ID（飞书 work_item_id）
     * @param pageNum         页码，从 1 开始
     * @param pageSize        每页条数
     * @return 评论列表，为 API 返回的 data 数组；失败返回空列表
     */
    public List<JsonNode> getWorkItemComments(String projectKey, String userEmail, String workItemTypeKey,
                                              String workItemId, int pageNum, int pageSize) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemTypeKey) || StringUtils.isBlank(workItemId)) {
            return Collections.emptyList();
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + workItemTypeKey + "/" + workItemId + "/comments"
                + "?page_num=" + Math.max(1, pageNum) + "&page_size=" + (pageSize <= 0 ? 20 : Math.min(100, pageSize));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    if (data.isArray()) {
                        List<JsonNode> list = new ArrayList<>();
                        data.forEach(list::add);
                        return list;
                    }
                    if (data.isObject()) {
                        JsonNode listNode = data.path("list");
                        if (listNode.isArray()) {
                            List<JsonNode> list = new ArrayList<>();
                            listNode.forEach(list::add);
                            return list;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 查询工作项评论失败: projectKey=" + projectKey + ", workItemId=" + workItemId + ", e=" + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 查询工作项操作记录（飞书 Open API：获取工作项操作记录），用于缺陷变更历史。
     *
     * @param projectKey      项目 key
     * @param userEmail       调用用户邮箱（为空则用 default-user-email）
     * @param workItemTypeKey 工作项类型 key，缺陷传 getDefectTypeKey()
     * @param workItemId      工作项 ID（飞书 work_item_id）
     * @param pageNum         页码，从 1 开始
     * @param pageSize        每页条数
     * @return 操作记录列表，为 API 返回的 data 数组；失败返回空列表
     */
    public List<JsonNode> getWorkItemOperations(String projectKey, String userEmail, String workItemTypeKey,
                                                String workItemId, int pageNum, int pageSize) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemTypeKey) || StringUtils.isBlank(workItemId)) {
            return Collections.emptyList();
        }
        LogUtils.info("[Feishu] getWorkItemOperations 调用: projectKey={}, workItemTypeKey={}, workItemId={}", projectKey, workItemTypeKey, workItemId);
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        // 按飞书「获取工作项操作记录」文档 / Postman 集合：POST /open_api/op_record/work_item/list
        String url = baseUrl + "/open_api/op_record/work_item/list";

        try {
            long wid;
            try {
                wid = Long.parseLong(workItemId);
            } catch (NumberFormatException e) {
                LogUtils.warn("[Feishu] 查询工作项操作记录失败: workItemId 非数字, workItemId={}", workItemId);
                return Collections.emptyList();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            Map<String, Object> body = new HashMap<>();
            // 按 Postman 示例体结构构造最小必需参数
            body.put("project_key", projectKey);
            body.put("work_item_ids", List.of(wid));
            body.put("start_from", "");
            body.put("operator", Collections.emptyList());
            body.put("operator_type", Collections.emptyList());
            body.put("source_type", Collections.emptyList());
            body.put("source", Collections.emptyList());
            body.put("operation_type", Collections.emptyList());
            body.put("start", 0);
            body.put("end", 0);
            body.put("op_record_module", Collections.emptyList());
            body.put("page_size", pageSize <= 0 ? 10 : Math.min(100, pageSize));

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);

            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    JsonNode data = root.path("data");
                    // 文档中 data 可能是数组或对象包裹 list，这里都兼容
                    if (data.isArray()) {
                        List<JsonNode> list = new ArrayList<>();
                        data.forEach(list::add);
                        return list;
                    }
                    if (data.isObject()) {
                        JsonNode listNode = data.path("list");
                        if (listNode.isArray()) {
                            List<JsonNode> list = new ArrayList<>();
                            listNode.forEach(list::add);
                            return list;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 查询工作项操作记录失败: projectKey=" + projectKey + ", workItemId=" + workItemId + ", e=" + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 工作项下创建评论并同步到飞书（飞书项目 Open API：POST 添加评论）
     * 路径以 Postman 集合「开放能力open-api接口」为准：/comment/create（非 /comment）。
     *
     * @param projectKey      项目 key
     * @param userEmail       调用用户邮箱（可为空，用于 X-USER-KEY）
     * @param workItemTypeKey 工作项类型 key，缺陷用 getDefectTypeKey()
     * @param workItemId      工作项 ID
     * @param content         评论内容（纯文本或富文本字符串）
     * @return 是否创建成功
     */
    public boolean addWorkItemComment(String projectKey, String userEmail, String workItemTypeKey,
                                      String workItemId, String content) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemTypeKey)
                || StringUtils.isBlank(workItemId) || StringUtils.isBlank(content)) {
            return false;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + workItemTypeKey + "/" + workItemId + "/comment/create";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);
            Map<String, Object> body = new HashMap<>();
            body.put("content", content);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            if (resp.getStatusCode() == HttpStatus.OK && StringUtils.isNotBlank(resp.getBody())) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (getResponseCode(root) == 0) {
                    LogUtils.info("[Feishu] 创建工作项评论成功: projectKey={}, workItemId={}", projectKey, workItemId);
                    return true;
                }
                LogUtils.warn("[Feishu] 创建工作项评论失败: code={}, msg={}", getResponseCode(root), getErrorMsg(root));
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 创建工作项评论异常: projectKey=" + projectKey + ", workItemId=" + workItemId + ", e=" + e.getMessage());
        }
        return false;
    }

    /**
     * 下载工作项附件/富文本中的图片（飞书 Open API：下载附件）
     * POST /open_api/:project_key/work_item/:work_item_type_key/:work_item_id/file/download，body: {"uuid": "..."}
     *
     * @param projectKey      项目 key
     * @param userEmail       调用用户邮箱（可为空）
     * @param workItemTypeKey 工作项类型 key，缺陷用 getDefectTypeKey()
     * @param workItemId      工作项 ID
     * @param fileUuid        图片/附件的 UUID（从 multi_texts[].field_value.doc 中解析）
     * @return 图片或文件字节及响应头（含 Content-Type），失败返回 null
     */
    public ResponseEntity<byte[]> downloadWorkItemFile(String projectKey, String userEmail, String workItemTypeKey,
                                                       String workItemId, String fileUuid) {
        if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemTypeKey)
                || StringUtils.isBlank(workItemId) || StringUtils.isBlank(fileUuid)) {
            return null;
        }
        String token = getPluginToken();
        String userKey = getUserKeyByEmail(resolveUserEmail(userEmail));
        String url = baseUrl + "/open_api/" + projectKey + "/work_item/" + workItemTypeKey + "/" + workItemId + "/file/download";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PLUGIN-TOKEN", token);
            headers.set("X-USER-KEY", userKey);

            Map<String, String> body = new HashMap<>();
            body.put("uuid", fileUuid);

            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), byte[].class);

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                HttpHeaders outHeaders = new HttpHeaders();
                outHeaders.setContentType(resp.getHeaders().getContentType() != null
                        ? resp.getHeaders().getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM);
                return new ResponseEntity<>(resp.getBody(), outHeaders, HttpStatus.OK);
            }
        } catch (Exception e) {
            LogUtils.warn("[Feishu] 下载工作项文件失败: projectKey=" + projectKey + ", workItemId=" + workItemId + ", uuid=" + fileUuid + ", e=" + e.getMessage());
        }
        return null;
    }
}
