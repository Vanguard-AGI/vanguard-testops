# 飞书缺陷双向同步 — 待办清单

> 与 [feishu-defect-migration-and-sync-analysis.md](./feishu-defect-migration-and-sync-analysis.md) 配套，用于跟踪剩余工作。

---

## 一、已完成 ✅

- [x] BugPlatform.FEISHU 枚举
- [x] FeishuMeegoService 缺陷 CRUD（create/update/delete/getDefectDetails/fetchAllDefects）
- [x] 我们 → 飞书：BugService.addOrUpdate 中 syncBugToFeishu（新增/修改）
- [x] 我们 → 飞书：BugService.delete() 单条删除时同步删飞书
- [x] 飞书 → 我们：FeishuWebhookController `POST /webhook/feishu/bug` + FeishuWebhookService 事件处理
- [x] 飞书 API 调用时 userEmail 未传则使用 defaultUserEmail（系统邮箱与飞书一致）
- [x] 迁移：FeishuDefectMigrationService + migrateFeishuDefects 接口
- [x] 鉴权：/webhook/feishu/** 已配置 anon
- [x] 配置项必填：commons.properties 已配置 default-user-email、project-key 等

---

## 二、待办（按优先级）

### P0 — 必做（否则功能不完整）

- [x] **批量删除支持 FEISHU**  
  - 位置：`BugService.batchDelete()`  
  - 已实现：在 `groupBugs.forEach` 中增加 `FEISHU` 分支，先按 `platformBugId` 调 `feishuMeegoService.deleteDefect`，再 `clearAssociateResource` + `bugMapper.deleteByExample`。

- [x] **配置项必填**  
  - 已在 `commons.properties` 中配置：`feishu.meego.base-url`、`plugin-id`、`plugin-secret`、`default-user-email`、`project-key`（defect-type-key 使用代码默认值即可）。

- [ ] **飞书侧配置 Webhook**  
  - 在飞书项目「自动化流程」中配置 Webhook。  
  - URL：`https://<公网域名>/webhook/feishu/bug`  
  - 确保该地址公网可访问（网关/反向代理需放行）。

### P1 — 建议做（体验与健壮性）

- [x] **Webhook 新建缺陷的 templateId**  
  - 已实现：`FeishuWebhookService.resolveTemplateId(projectId)` 通过 `ProjectTemplateService.getOption(projectId, TemplateScene.BUG)` 取该项目默认缺陷模板（enableDefault），返回其 ID；查不到或异常时返回空串。

- [ ] **Webhook 事件 work_item_type_key 兼容**  
  - 位置：`FeishuWebhookService.dispatch()` 中对 `work_item_type_key` 的判断。  
  - 说明：当前只与 `getDefectTypeKey()`（如 `63329b6c980d67099b12fd73`）比较，飞书可能推送 `"defect"` 等别名。  
  - 做法：当 `work_item_type_key` 等于 getDefectTypeKey() 或等于 `"defect"` 时均视为缺陷事件。

### P2 — 可选

- [ ] **迁移入口**  
  - 若需要从飞书拉存量缺陷：调用已有 `migrateFeishuDefects` 接口；前端可在「项目设置」或「缺陷管理」加「从飞书迁移」按钮。

- [ ] **前端**  
  - 若 TestPlanCaseDetailPage 的「添加缺陷」「取消关联」尚未绑定或列表字段（如 name/severity）与后端不一致，按分析文档第五章做适配。

- [ ] **监控与重试**  
  - Webhook 处理日志/失败重试（如 Spring Retry 或自定义）；飞书调用失败可记录日志便于排查。

---

## 三、实施顺序建议

1. 配置 `default-user-email`、`project-key`（本地/测试先打通）。
2. 实现 **batchDelete 的 FEISHU 分支**（避免批量删飞书缺陷时走错逻辑）。
3. 实现 **resolveTemplateId**、**work_item_type_key 兼容**。
4. 在飞书项目配置 **Webhook URL**，验证飞书 → 我们 的同步。
5. 按需做迁移入口、前端与监控。

---

## 四、相关文件速查

| 事项           | 文件 |
|----------------|------|
| 批量删除       | `vanguard-testops-service/src/main/java/io/metersphere/bug/service/BugService.java` → `batchDelete` |
| Webhook 模板 ID | `vanguard-testops-service/src/main/java/io/metersphere/bug/service/FeishuWebhookService.java` → `resolveTemplateId` |
| 事件类型兼容   | `vanguard-testops-service/src/main/java/io/metersphere/bug/service/FeishuWebhookService.java` → `dispatch` |
| 配置           | `start/src/main/resources/commons.properties`（或当前环境配置） |
| Webhook 入口   | `FeishuWebhookController` → `POST /webhook/feishu/bug` |

完成 P0 三项后，双向同步即可正常使用；P1/P2 按需排期。
