# 效能指标定时任务配置说明

## 📋 定时任务清单

### 1. 测试计划指标计算
- **任务类**: `io.metersphere.plan.job.TestPlanMetricsScheduledJob`
- **配置项**: `metrics.schedule.test-plan`
- **默认时间**: 每天凌晨3点 (`0 0 3 * * ?`)
- **执行方式**: 异步执行，不阻塞主线程

### 2. 用例CS值计算
- **任务类**: `io.metersphere.functional.job.CaseMetricsScheduledJob.calculateCaseCS()`
- **配置项**: `metrics.schedule.case-cs`
- **默认时间**: 每天凌晨2点 (`0 0 2 * * ?`)
- **执行方式**: 异步执行

### 3. 用例效能指标计算
- **任务类**: `io.metersphere.functional.job.CaseMetricsScheduledJob`
- **每日指标**: `metrics.schedule.case-metrics-daily` (凌晨4点)
- **每周指标**: `metrics.schedule.case-metrics-weekly` (周一凌晨4点)
- **每月指标**: `metrics.schedule.case-metrics-monthly` (每月1号凌晨4点)

### 4. 飞书需求同步
- **任务类**: `io.metersphere.functional.job.CaseMetricsScheduledJob.syncFeishuStories()`
- **配置项**: `metrics.schedule.feishu-sync`
- **默认时间**: 每30分钟 (`0 */30 * * * ?`)
- **前置条件**: 需要配置 `feishu.meego.default-user-email`
- **执行方式**: 异步执行

## ⚙️ 配置方式

### 方法1：在 application.properties 中配置

```properties
# 修改测试计划指标计算时间为凌晨1点
metrics.schedule.test-plan=0 0 1 * * ?

# 修改CS值计算时间为凌晨2点30分
metrics.schedule.case-cs=0 30 2 * * ?

# 修改飞书同步频率为每小时一次
metrics.schedule.feishu-sync=0 0 */1 * * ?
```

### 方法2：通过环境变量配置

```bash
# Docker 环境
docker run -e "METRICS_SCHEDULE_TEST_PLAN=0 0 1 * * ?" ...

# Kubernetes ConfigMap
METRICS_SCHEDULE_TEST_PLAN: "0 0 1 * * ?"
```

### 方法3：配置文件已内置

配置文件已包含在当前服务模块中：
- `vanguard-testops-service/src/main/resources/application-metrics.properties`

## ⏰ 推荐的执行时间安排

```
02:00 - 用例CS值计算        （基础数据）
03:00 - 测试计划指标计算     （依赖CS数据）
04:00 - 用例效能指标计算     （日/周/月）
每30分钟 - 飞书需求同步      （实时性要求高）
```

## 📝 Cron 表达式示例

| 表达式 | 说明 |
|--------|------|
| `0 0 0 * * ?` | 每天凌晨0点 |
| `0 0 3 * * ?` | 每天凌晨3点 |
| `0 */30 * * * ?` | 每30分钟 |
| `0 0 */2 * * ?` | 每2小时 |
| `0 0 0 ? * MON` | 每周一凌晨0点 |
| `0 0 0 1 * ?` | 每月1号凌晨0点 |

## 🔧 临时禁用某个任务

### 方法1：设置为不可能的时间
```properties
# 禁用测试计划指标计算（2月31日不存在）
metrics.schedule.test-plan=0 0 0 31 2 ?
```

### 方法2：使用 Spring Profile
```properties
# 在 application-prod.properties 中禁用开发环境的定时任务
metrics.schedule.test-plan=-
```

## 🚨 注意事项

1. **时区**: Cron 表达式使用服务器本地时区
2. **异步执行**: 所有任务都使用异步线程池，不会阻塞主线程
3. **数据依赖**: CS值计算应该在测试计划指标计算之前执行
4. **飞书配置**: 需要配置 `feishu.meego.default-user-email` 才会执行同步任务

## 📊 监控日志

所有任务都会记录详细日志：

```
========== 定时任务：测试计划指标计算 ==========
测试计划指标计算任务已提交至异步线程池
========== 异步批量计算任务开始 ==========
...
========== 异步批量计算任务完成：成功 387, 失败 0, 总数 388 ==========
```

搜索关键字：`========== 定时任务` 查看所有定时任务的执行记录。
