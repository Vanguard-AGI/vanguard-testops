# 缺陷从飞书迁移到我们平台 + 双向同步（Webhook 保证时效性）—— 全面代码对照分析

> 本文档在原方案基础上，逐一对照**后端代码**、**前端代码**、**飞书 Open API（Postman 集合）**，对每个 TODO 标注「**现有可复用** / **需新增** / **需改造**」，并给出改造要点。

---

## 一、后端现有代码结构概览

### 1.1 核心数据模型

| 类/表 | 路径 | 核心字段 | 与飞书对接关系 |
|--------|------|----------|---------------|
| **Bug** | `framework/domain/.../Bug.java` | `id`(VARCHAR50), `num`(INT), `title`, `handleUser`, `createUser`, `createTime`, `updateTime`, `projectId`, `templateId`, **`platform`**(VARCHAR50), `status`, `tags`, **`platformBugId`**(VARCHAR50), `deleted`, `pos` | ✅ **已有 `platform` + `platformBugId`**，直接用于存飞书映射，无需新建表 |
| **BugContent** | `framework/domain/.../BugContent.java` | `bugId`(VARCHAR50), `description`(LONGTEXT) | 飞书描述字段映射 |
| **BugRelationCase** | `framework/domain/.../BugRelationCase.java` | `id`, `caseId`, `bugId`(VARCHAR50), `caseType`, `testPlanId`, `testPlanCaseId`, `createUser`, `createTime` | `bugId` 指向本地 `bug.id`，**INNER JOIN bug**；飞书缺陷必须落本地 bug 表才能展示 |

### 1.2 BugPlatform 枚举

```java
// BugPlatform.java
LOCAL("Local"), JIRA("JIRA"), ZENTAO("禅道"), TAPD("TAPD")
```

⚠️ **需新增** `FEISHU("FEISHU")` 枚举值。

### 1.3 BugService 核心流程

| 方法 | 行号 | 流程概要 | 与飞书对接要点 |
|------|------|---------|---------------|
| `addOrUpdate()` | L201 | ① 判断平台 → ② 非 Local 则调第三方 Platform 插件 → ③ `handleAndSaveBugAndNotice` 保存基础信息 → ④ 自定义字段 → ⑤ 附件 → ⑥ 富文本临时文件 → ⑦ `handleAndSaveCaseRelation` 写关联 | 现有 Platform 插件体系用于 JIRA/TAPD 等，**飞书走独立 Service 不走插件**，需在步骤②之后/之前插入「调飞书 API」逻辑 |
| `handleAndSaveBugAndNotice()` | L1004 | 非 Local：从 `platformBug.getPlatformBugKey()` 回写 `bug.platformBugId`；Local：`platformBugId=null` | **可复用**：飞书创建后同样用此回写 `platformBugId` |
| `handleAndSaveCaseRelation()` | L1423 | 创建时若 `request.caseId` 非空，则插入 `BugRelationCase` | **可直接复用**，不需改造 |
| `delete()` | L318 | Local 软删；非 Local 先调 `platform.deleteBug()`，再 `clearAssociateResource` + `deleteByPrimaryKey` | **需改造**：增加 `FEISHU` 平台判断，调飞书 DELETE API |
| `get()` | L255 | 查 bug + template + customFields + content + attachments | **可直接复用** |
| `batchDelete()` | L408 | 按 platform 分组处理 | **需改造**：加入 FEISHU 分组处理逻辑 |
| `syncPlatformAllBugs()` / `doSyncPlatformBugs()` | L589/L639 | 现有平台同步机制（拉取 → 比对 → 批量更新/删除） | 飞书用 Webhook 而非轮询，**不复用此流程** |

### 1.4 BugController 接口全表

| HTTP | 路径 | 说明 | 复用情况 |
|------|------|------|---------|
| POST | `/bug/add` | 创建缺陷（multipart） | ✅ 可复用，前端走此接口，后端拦截 FEISHU 平台做同步 |
| POST | `/bug/update` | 更新缺陷 | ✅ 同上 |
| GET | `/bug/delete/{id}` | 删除缺陷 | ✅ 同上 |
| GET | `/bug/get/{id}` | 缺陷详情 | ✅ 直接复用 |
| POST | `/bug/page` | 缺陷列表 | ✅ 直接复用 |
| POST | `/bug/batch-delete` | 批量删除 | ✅ 需改造内部逻辑 |
| GET | `/bug/sync/{projectId}` | 同步（存量） | 飞书不用 |
| POST | `/bug/sync/all` | 全量同步 | 飞书不用 |
| POST | `/bug/attachment/upload` | 上传附件 | ✅ 复用 |

### 1.5 关联缺陷查询链路（核心！）

**SQL `getTestPlanAssociateBugs`**（`ExtBugRelateCaseMapper.xml` L281-317）：

```sql
SELECT brc.id, brc.bug_id, b.num, b.title AS name, b.handle_user, b.status, ...
FROM bug_relation_case brc
INNER JOIN bug b ON brc.bug_id = b.id        -- ⚠️ INNER JOIN！
INNER JOIN bug_content bc ON brc.bug_id = bc.bug_id
WHERE b.deleted = false
  AND brc.test_plan_case_id = #{request.testPlanCaseId}
```

**结论**：飞书缺陷**必须**存入 `bug` + `bug_content` 表，否则 INNER JOIN 查不出来。本方案正确。

**`BugProviderDTO`** 返回字段：`id`, `num`, `bugId`, `name`(title别名), `handleUser`, `handleUserName`, `status`, `statusName`, `content`, `createUser`, `createUserName`, `tags`, `testPlanName`, `source`。

⚠️ 前端使用了 `bug.title`、`bug.severity`，但后端 DTO 中 **没有 `severity` 字段**，title 映射为 `name`。需确认前端字段适配。

### 1.6 FeishuMeegoService 现有能力

| 能力 | 状态 | 说明 |
|------|------|------|
| `getPluginToken()` | ✅ 已有 | 获取 X-PLUGIN-TOKEN，含缓存（110分钟） |
| `getUserKeyByEmail()` | ✅ 已有 | 通过邮箱获取 X-USER-KEY，含缓存 |
| `fetchWorkItemsByProject()` | ✅ 已有 | 按 project_key + type_key 分页拉取工作项（filter 接口） |
| `DEFECT_TYPE_KEY` | ✅ 已有 | `"63329b6c980d67099b12fd73"` |
| **创建工作项** | ❌ 缺失 | 需新增 |
| **更新工作项** | ❌ 缺失 | 需新增 |
| **删除工作项** | ❌ 缺失 | 需新增 |
| **查询工作项详情** | ❌ 缺失 | 需新增（query 接口） |
| **文件上传到飞书** | ❌ 缺失 | 需新增（TODO-2.5 图片） |
| **Webhook 接收处理** | ❌ 缺失 | 需新增整个模块 |

RestTemplate 已注入（`feishuRestTemplate`），ObjectMapper 已有，headers 构建模式已建立——新增方法可参照现有模式。

---

## 二、前端现有代码结构概览

### 2.1 缺陷相关文件清单

| 文件 | 用途 | 与飞书对接 |
|------|------|-----------|
| `pages/BugManagementPage.tsx` | 缺陷管理主页 | 🚧 目前 `SHOW_COMING_SOON = true`，功能未开放；列表调 `bugManagementService.getBugList`→`POST /bug/page` |
| `pages/TestPlanCaseDetailPage.tsx` | 测试计划用例详情（含缺陷列表 Tab） | 🔑 关键页面，"添加缺陷"按钮 **无 onClick**，"取消关联"按钮 **无 onClick** |
| `components/features/test-plan/PlanDetailDefect.tsx` | 计划详情-缺陷列表 Tab | ✅ 已有「关联缺陷」弹窗入口 + 取消关联 |
| `components/features/test-plan/AssociateBugDialog.tsx` | 关联缺陷弹窗 | ✅ 已实现选择缺陷 + 调 `testPlanAssociateBug` API |
| `components/features/case-management/components/drawer-tabs/TabBug.tsx` | 用例管理-缺陷 Tab | 相关但非核心改动点 |
| `components/features/case-management/components/drawer-tabs/LinkDefectDrawer.tsx` | 关联缺陷抽屉 | 相关 |
| `services/bug-management/service.ts` | 缺陷 CRUD 服务 | ✅ 已有 `createOrUpdateBug`, `getBugDetail`, `deleteSingleBug` 等 |
| `services/bug-management/constants/urls.ts` | 缺陷 URL 常量 | ✅ 已完整 |
| `services/test-plan/service.ts` | 测试计划服务 | ✅ 已有 `getAssociatedBug`, `testPlanAssociateBug`, `testPlanCancelBug` |
| `services/test-plan/constants/urls.ts` | 测试计划 URL 常量 | ✅ 已完整 |

### 2.2 TestPlanCaseDetailPage 缺陷相关分析

| 元素 | 代码行 | 状态 | 改造 |
|------|--------|------|------|
| `fetchAssociatedBugs()` | L363-377 | ✅ 已实现，调 `getAssociatedBug` → `POST /test-plan/functional/case/has/associate/bug/page` | 不需改 |
| 缺陷列表渲染 | L876-886 | 使用 `bug.num`, `bug.severity`, `bug.title` | ⚠️ 后端 `BugProviderDTO` 无 `severity`/`title`；`title` 实际返回为 `name`→ **需前端适配或后端加别名** |
| "添加缺陷"按钮 | L856-858 | **无 onClick** | **需改造**：绑定打开 `AssociateBugDialog` 或新建缺陷弹窗 |
| "取消关联"按钮 | L883 | **无 onClick** | **需改造**：调 `testPlanCancelBug` |
| `AssociateBugDialog` | 独立组件 | ✅ 已实现 | 可直接引入到 TestPlanCaseDetailPage |

### 2.3 前端服务 API 覆盖度

| 操作 | 前端方法 | 后端接口 | 状态 |
|------|---------|---------|------|
| 创建缺陷 | `bugManagementService.createOrUpdateBug` | `POST /bug/add` | ✅ |
| 更新缺陷 | `bugManagementService.updateBug` | `POST /bug/update` | ✅ |
| 删除缺陷 | `bugManagementService.deleteSingleBug` | `GET /bug/delete/{id}` | ✅ |
| 缺陷详情 | `bugManagementService.getBugDetail` | `GET /bug/get/{id}` | ✅ |
| 缺陷列表 | `bugManagementService.getBugList` | `POST /bug/page` | ✅ |
| 关联缺陷到用例 | `testPlanManagementService.testPlanAssociateBug` | `POST /test-plan/functional/case/associate/bug` | ✅ |
| 取消关联 | `testPlanManagementService.testPlanCancelBug` | `POST /test-plan/functional/case/disassociate/bug` | ✅ |
| 获取关联缺陷列表 | `testPlanManagementService.getAssociatedBug` | `POST /test-plan/functional/case/has/associate/bug/page` | ✅ |
| Webhook 接收 | - | - | ❌ **需后端新增** |
| 飞书同步状态查看 | - | - | ❌ 可选 |

---

## 三、飞书 Open API 接口清单（Postman 集合对照）

### 3.1 工作项 CRUD

| 接口名称 | 方法 | 路径 | Postman 行 | TODO 对应 |
|----------|------|------|-----------|-----------|
| 创建工作项 | POST | `/{project_key}/work_item/create` | L1772 | TODO-1.1 |
| 更新工作项 | PUT | `/{project_key}/work_item/:type_key/:id` | L1811 | TODO-1.2 |
| 批量更新工作项字段值 | PUT | `/{project_key}/work_item/:type_key/batch_update` | L1851 | 可选优化 |
| 删除工作项 | DELETE | `/{project_key}/work_item/:type_key/:id` | L1957 | TODO-1.3 |
| 获取工作项详情 | POST | `/{project_key}/work_item/:type_key/query` | L1700 | TODO-1.4 |
| 获取创建工作项元数据 | GET | `/{project_key}/field/:type_key/meta` | L1740 | 字段映射 |
| 获取工作项列表（单空间） | POST | `/{project_key}/work_item/filter` | L1498 | TODO-3（迁移） |
| 获取工作项列表（跨空间） | POST | `/work_items/filter_across_project` | L1537 | 备选 |
| 终止/恢复工作项 | PUT | `/{project_key}/work_item/:type_key/:id/abort_or_recover` | L1997 | 可选 |

### 3.2 附件/文件接口

| 接口名称 | 方法 | 路径 | Postman 行 | TODO 对应 |
|----------|------|------|-----------|-----------|
| 添加附件 | POST | `/{project_key}/work_item/story/:id/file/upload` | L144 | TODO-2.5 |
| 文件上传 | POST | `/open_api/file/upload` | L208 | TODO-2.5 备选 |
| 下载附件 | GET | - | L254 | TODO-3.3 |
| 删除附件 | DELETE | - | L302 | 可选 |

### 3.3 用户接口

| 接口名称 | 方法 | 路径 | 现有代码 |
|----------|------|------|---------|
| 通过邮箱查用户 | POST | `/open_api/user/query` | ✅ `getUserKeyByEmail()` 已有 |

---

## 四、逐 TODO 代码对照与改造方案

### TODO-1：飞书缺陷 CRUD 封装

#### TODO-1.1 创建工作项

- **现有代码**：`FeishuMeegoService` 已有 `getPluginToken()`、`getUserKeyByEmail()`、`RestTemplate` 注入、请求模板
- **需新增**：`createDefect(String projectKey, String userEmail, String title, String description, Map<String, Object> extraFields) → String workItemId`
- **实现参照**：参照 `fetchWorkItemsByProject()` 的请求构建模式
- **飞书 API**：`POST /{project_key}/work_item/create`，body: `{ work_item_type_key, field_value_pairs: [{field_key: "field_xxx", field_value: ...}], name }`，Header: `X-USER-KEY`

#### TODO-1.2 更新工作项

- **需新增**：`updateDefect(String projectKey, String userEmail, String workItemId, List<Map> updateFields)`
- **飞书 API**：`PUT /{project_key}/work_item/{DEFECT_TYPE_KEY}/{workItemId}`，body: `{ update_fields: [{field_key, field_value}] }`

#### TODO-1.3 删除工作项

- **需新增**：`deleteDefect(String projectKey, String userEmail, String workItemId)`
- **飞书 API**：`DELETE /{project_key}/work_item/{DEFECT_TYPE_KEY}/{workItemId}`

#### TODO-1.4 查询工作项详情

- **现有代码**：`fetchWorkItemsByProject()` 可查列表，但无按 ID 查详情
- **需新增**：`getDefectDetail(String projectKey, String userEmail, List<Long> workItemIds)`
- **飞书 API**：`POST /{project_key}/work_item/{DEFECT_TYPE_KEY}/query`，body: `{ work_item_ids: [...] }`

**复用度评估**：认证/请求基础设施 70% 可复用，CRUD 方法体需全部新写。

---

### TODO-2：我们侧缺陷与飞书联动（我们 → 飞书）

#### TODO-2.1 新增缺陷同步到飞书

**改造点 —— `BugService.addOrUpdate()` (L201-247)**

```
现有流程：
① getPlatformName → ② 非Local调Platform插件 → ③ handleAndSaveBugAndNotice → ④ customFields → ⑤ attachment → ⑥ richText → ⑦ caseRelation

改造后流程（在 ② 之后/平行插入飞书逻辑）：
② 判断 platform == "FEISHU" ?
  → 是：调 FeishuDefectSyncService.createDefect()，获得 workItemId
  → 构造 PlatformBugUpdateDTO（setPlatformBugKey = workItemId）
  → 进入 ③ handleAndSaveBugAndNotice（回写 platformBugId）
③ - ⑦ 不变
```

具体改造位置：`BugService.java` L212-234 的 `if (!StringUtils.equals(platformName, BugPlatform.LOCAL.getName()))` 块中，加一个 `else if (StringUtils.equals(platformName, "FEISHU"))` 分支。

**或者**（推荐）：在 `handleAndSaveBugAndNotice()` 之后，新增一个 `syncToFeishuIfNeeded(bug, request)` 方法，逻辑更解耦。

#### TODO-2.2 修改缺陷同步到飞书

同上，在 `addOrUpdate(isUpdate=true)` 路径中，`handleAndSaveBugAndNotice` 之后，若 `bug.platform == FEISHU && bug.platformBugId != null`，调 `updateDefect()`。

#### TODO-2.3 删除缺陷同步到飞书

**改造点 —— `BugService.delete()` (L318-345)**

```
现有：Local → 软删；非Local → 若平台一致则调 platform.deleteBug()
改造：增加 FEISHU 判断：
  if (platform == "FEISHU" && platformBugId != null) {
      feishuDefectSyncService.deleteDefect(projectKey, userEmail, platformBugId);
  }
  bugCommonService.clearAssociateResource(...)
  bugMapper.deleteByPrimaryKey(id)
```

#### TODO-2.4 确定触发点

| 操作 | 触发点 | 调用方式 |
|------|--------|---------|
| 新增 | `BugService.addOrUpdate(isUpdate=false)` 之后 | 同步调用（失败可降级记录日志） |
| 修改 | `BugService.addOrUpdate(isUpdate=true)` 之后 | 同步调用 |
| 删除 | `BugService.delete()` / `batchDelete()` | 同步或异步（已有虚拟线程模式可参照 L450） |

#### TODO-2.5 图片展示

- **现有代码**：富文本图片通过 `bug/attachment/preview/md/{projectId}/{fileId}` 存储和访问
- **需验证**：飞书工作项描述是否支持外部 URL 图片
- **若不支持需新增**：
  1. 从 OSS 下载图片
  2. 调飞书 `POST /{project_key}/work_item/story/{id}/file/upload`（或 `file/upload`） 上传
  3. 用飞书返回的文件引用替换描述中的图片 URL

---

### TODO-3：迁移任务

#### TODO-3.1 拉取飞书缺陷列表

- **现有可复用**：`fetchWorkItemsByProject(token, userKey, projectKey, typeKey)` 已有完整的分页拉取逻辑
- **改造**：`DEFECT_TYPE_KEY` 已定义为 `63329b6c980d67099b12fd73`，直接传入即可

#### TODO-3.2 写入本地

- **需新增**：迁移方法 `migrateFeishuDefects(projectKey, ourProjectId, templateId, userEmail)`
- 每条飞书缺陷：
  1. 构建 `Bug` 对象：`platform="FEISHU"`, `platformBugId=workItemId`, title/status/handleUser 从飞书字段映射
  2. 插入 `bug`（用 `bugMapper.insert()`）
  3. 插入 `bug_content`（用 `bugContentMapper.insert()`）
  4. 幂等：`SELECT * FROM bug WHERE platform='FEISHU' AND platform_bug_id=?`，存在则跳过/更新

#### TODO-3.3 附件处理

- 从飞书下载 → 上传 OSS → 存 OSS URL 到 `bug_content` 或附件表
- **现有可参照**：`BugAttachmentService.syncAttachmentToMs()` + `FileCenter` 体系

#### TODO-3.4 管理端入口

- **需新增**：后端 Controller 接口 `POST /feishu/defect/migrate`（参数：projectKey, ourProjectId, templateId, userEmail）
- 前端可选：在「项目设置」或「缺陷管理」页面加一个「从飞书迁移」按钮

---

### TODO-4/5：Webhook 接收（飞书 → 我们）

#### TODO-4.1 接口定义

- **需新增整个 Controller**，例如 `FeishuWebhookController`
- 路径：`POST /api/feishu/project/webhook`
- 此接口需**公网可访问**，不走平台鉴权（飞书直接调用）

#### TODO-4.2 url_verification

```java
@PostMapping("/api/feishu/project/webhook")
public Object handleWebhook(@RequestBody Map<String, Object> body) {
    if (body.containsKey("challenge")) {
        return Map.of("challenge", body.get("challenge"));
    }
    // 异步处理事件
    asyncProcessEvent(body);
    return Map.of("code", 0);
}
```

#### TODO-4.3 事件处理

- **需新增**：`FeishuWebhookEventService`
- 解析 `event_type` + `payload.work_item_id`
- 查 `bug`（`WHERE platform='FEISHU' AND platform_bug_id=?`）
- 更新/软删/新建

#### TODO-5.1 幂等

- **需新增**：Redis 或 DB 记录已处理的 `uuid`/`event_id`，TTL 24h

#### TODO-5.2 事件分发

| 事件类型 | 我们侧操作 |
|---------|-----------|
| WorkitemUpdateEvent / WorkitemStatusEvent | 拉飞书详情 → 更新 `bug` + `bug_content` |
| WorkitemDeleteEvent | 软删 `bug`（`deleted=true`） |
| WorkitemAbortedEvent | 更新 `bug.status` |
| WorkitemCreateEvent | 若 `platform_bug_id` 不存在 → 调详情 API 在我们侧新建 `bug` |

---

### TODO-6：飞书项目侧配置

- **纯配置操作**，不涉及代码
- 在飞书项目「自动化流程」中配置 Webhook 触发器 + URL

---

### TODO-7：配置与安全

#### TODO-7.1 配置可化

- **现有**：`FeishuMeegoService` 已用 `@Value` 注入 `base-url`, `plugin-id`, `plugin-secret`, `default-user-email`
- **需新增**：`feishu.meego.project-key`, `feishu.meego.our-project-id`, `feishu.meego.our-template-id` 等配置项
- 配置文件：`start/src/main/resources/commons.properties`（已在 git 变更列表中）

#### TODO-7.2 Webhook 签名校验

- **需新增**：若飞书 Webhook 带签名 header，在 Controller 中校验

#### TODO-7.3 附件统一 OSS

- **现有可复用**：`BugAttachmentService`、`FileCenter`、`FileMetadataService` 整套 OSS 体系
- 不需要额外改造

---

### TODO-8/9：监控与文档

- TODO-8.1：**需新增** Webhook 日志表或日志输出
- TODO-8.2：**需新增** 飞书调用失败重试机制（可用 Spring Retry 或自定义）
- TODO-9：文档

---

## 五、前端改造清单

### 5.1 TestPlanCaseDetailPage.tsx 改造

| 改动项 | 当前状态 | 改造内容 |
|--------|---------|---------|
| "添加缺陷"按钮 (L856-858) | 无 onClick | 绑定 `onClick={() => setAssociateBugOpen(true)}`，引入 `AssociateBugDialog` |
| "取消关联"按钮 (L883) | 无 onClick | 绑定 `onClick={() => handleDisassociateBug(bug.id)}`，调 `testPlanCancelBug` |
| 缺陷列表字段 (L879-881) | `bug.num`, `bug.severity`, `bug.title` | 后端返回 `name`（不是 `title`），无 `severity` → **需适配为 `bug.name`**，`severity` 需从自定义字段或状态映射 |
| 引入 `AssociateBugDialog` | 未引入 | 需 import 并加状态变量 `associateBugOpen` |
| 关联成功后刷新 | - | `onSuccess={() => fetchAssociatedBugs()}` |

### 5.2 BugManagementPage.tsx

- 当前 `SHOW_COMING_SOON = true`，功能未开放
- 若需要完整的缺陷管理页面，需要：
  1. 关闭 `SHOW_COMING_SOON`
  2. 完善列表加载、创建/编辑弹窗等
  3. 创建/编辑时需选择平台（Local / FEISHU）

### 5.3 AssociateBugDialog.tsx

- **已基本可用**，但当前调用的是 `getPlanDetailBugPage`（计划维度）而非缺陷全量列表
- 可能需要改为调 `POST /bug/page` 获取所有可关联缺陷
- 关联时传参需包含 `caseId`, `testPlanCaseId`

### 5.4 PlanDetailDefect.tsx

- **已完善**，关联缺陷弹窗和取消关联都已实现
- 字段适配同 5.1

---

## 六、字段映射表（我们 ↔ 飞书）

| 我们字段 | 飞书字段 | 说明 |
|---------|---------|------|
| `bug.title` | `name` / `field_value_pairs` 中 `name` 字段 | 标题 |
| `bug_content.description` | `field_value_pairs` 中描述字段（需 meta 接口确认 field_key） | 富文本描述 |
| `bug.status` | 飞书工作流状态 | 需维护状态值映射表 |
| `bug.handleUser` | `X-USER-KEY` / 处理人字段 | 飞书用 user_key |
| `bug.createUser` | `X-USER-KEY` (创建时 Header) | 创建人 |
| `bug.platformBugId` | `work_item_id` | 唯一映射 |
| `bug.platform` | 固定 `"FEISHU"` | 平台标识 |
| `bug.tags` | 飞书标签字段（若有） | 可选 |

---

## 七、实施优先级与工作量估算

| 阶段 | TODO | 复用度 | 预估工作量 | 优先级 |
|------|------|--------|-----------|--------|
| 基础 | TODO-1 (飞书CRUD封装) | 基础设施70%复用 | 2-3天 | P0 |
| 联动 | TODO-2 (我们→飞书同步) | BugService改造 | 2-3天 | P0 |
| 前端 | 前端改造（按钮绑定+字段适配） | 组件已有 | 1天 | P0 |
| 迁移 | TODO-3 (一次性迁移) | fetchWorkItems可复用 | 2天 | P1 |
| Webhook | TODO-4+5 (飞书→我们) | 需全新开发 | 3-4天 | P1 |
| 配置 | TODO-6+7 (飞书配置+安全) | 配置+简单代码 | 1天 | P2 |
| 监控 | TODO-8+9 | 新增日志+文档 | 1-2天 | P2 |

**总计估算：12-16 个工作日**

---

## 八、风险与注意事项

1. **BugPlatform 枚举缺少 FEISHU**：`BugService.delete()`、`recover()`、`deleteTrash()` 中有 `if (platform == LOCAL)` 的硬判断，非 Local 的删除逻辑会走第三方平台分支。FEISHU 不走 Platform 插件体系，需独立处理。
2. **前端字段不匹配**：`BugProviderDTO.name` vs 前端 `bug.title`，`bug.severity` 后端无此字段。需前端统一使用 `name` 或后端 SQL 加 `b.title as title` 别名。
3. **Platform 插件体系**：现有 JIRA/TAPD 走 `Platform` SPI 接口（`platform.addBug()`），飞书如果不走插件需要在 `addOrUpdate()` 中增加独立分支，可能增加方法复杂度。建议抽取为独立的 `FeishuDefectSyncService`，在 `BugService` 中通过依赖注入调用。
4. **DEFECT_TYPE_KEY 硬编码**：当前 `63329b6c980d67099b12fd73` 是特定飞书空间的缺陷类型 key，不同空间可能不同。需提取为配置。
5. **platformBugId 长度**：`VARCHAR(50)`，飞书 work_item_id 通常为纯数字 ID（如 `1234567890`），长度足够。
6. **Webhook 公网可达**：需确保部署环境有公网入口或配置反向代理，飞书才能推送到我们的 Webhook URL。

---

## 九、数据流架构图（完整版）

```
┌─────────────────────┐     ┌──────────────────────────────────┐     ┌──────────────────────┐
│    前端 (React)      │     │         后端 (Spring Boot)          │     │    飞书项目           │
│                     │     │                                    │     │                      │
│ BugManagementPage   │────▶│ BugController                      │     │                      │
│ TestPlanCaseDetail  │     │   └─ BugService.addOrUpdate()      │     │                      │
│ AssociateBugDialog  │     │       ├─ handleAndSaveBugAndNotice  │     │                      │
│ PlanDetailDefect    │     │       ├─ handleAndSaveCustomFields  │     │                      │
│                     │     │       ├─ handleAndSaveAttachment    │     │                      │
│                     │     │       ├─ handleAndSaveCaseRelation  │     │                      │
│                     │     │       └─ [NEW] syncToFeishu()  ────────▶ │ POST work_item/create │
│                     │     │                                    │     │ PUT  work_item/update │
│                     │     │   └─ BugService.delete()           │     │ DELETE work_item      │
│                     │     │       └─ [NEW] deleteFromFeishu()──────▶ │                      │
│                     │     │                                    │     │                      │
│                     │     │ [NEW] FeishuWebhookController      │◀─── │ Webhook POST         │
│                     │     │   └─ FeishuWebhookEventService     │     │ (自动化流程)           │
│                     │     │       ├─ 更新 bug/bug_content      │     │                      │
│                     │     │       └─ 软删 bug                  │     │                      │
│                     │     │                                    │     │                      │
│                     │     │ FeishuMeegoService (现有+扩展)      │     │                      │
│                     │     │   ├─ getPluginToken() ✅            │     │                      │
│                     │     │   ├─ getUserKeyByEmail() ✅         │     │                      │
│                     │     │   ├─ [NEW] createDefect()          │     │                      │
│                     │     │   ├─ [NEW] updateDefect()          │     │                      │
│                     │     │   ├─ [NEW] deleteDefect()          │     │                      │
│                     │     │   └─ [NEW] getDefectDetail()       │     │                      │
│                     │     │                                    │     │                      │
│                     │     │ 数据层                              │     │                      │
│                     │     │   bug (platform=FEISHU,             │     │                      │
│                     │     │        platform_bug_id=飞书ID)      │     │                      │
│                     │     │   bug_content                      │     │                      │
│                     │     │   bug_relation_case                 │     │                      │
└─────────────────────┘     └──────────────────────────────────┘     └──────────────────────┘
```
