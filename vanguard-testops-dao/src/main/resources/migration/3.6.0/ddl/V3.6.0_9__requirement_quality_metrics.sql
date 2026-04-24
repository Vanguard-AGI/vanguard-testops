/*
 * ======================================================================================
 * 数据库：spotter_efficiency (质量效能平台)
 * 版本：V3.6.0_9
 * 作用：需求质量视图 - 库表已确定，仅含以下两项，无其他表：
 *       ① 流水线事实表 requirement_change_stats
 *       ② 需求表 meego_story_stats 扩展字段
 * ======================================================================================
 */

-- 1. 流水线事实表 requirement_change_stats（接收运维脚本上报，与需求表通过 story_id 关联）
--    定时任务按 story_id 从本表聚合写入 meego_story_stats：frontend_loc_changed=SUM(loc_valid) WHERE endpoint_type='FRONTEND'，
--    backend_loc_changed=SUM(loc_valid) WHERE endpoint_type='BACKEND'；有效变更行数(汇总)不存，由 frontend_loc_changed+backend_loc_changed 实时汇总；
--    deploy_total_count=COUNT(DISTINCT pipeline_id)，deploy_failure_count/last_deploy_time/change_failure_rate 等。详见文档 2.5 节。
CREATE TABLE IF NOT EXISTS spotter_efficiency.`requirement_change_stats` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `story_id` VARCHAR(100) NOT NULL COMMENT '需求ID (Story ID)，关联 meego_story_stats.story_id',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `service_id` VARCHAR(100) NOT NULL COMMENT '微服务/应用ID',
    `endpoint_type` VARCHAR(20) NOT NULL COMMENT 'FRONTEND/BACKEND/MIXED',
    `pipeline_id` VARCHAR(100) NOT NULL COMMENT '流水线运行ID',
    `pipeline_name` VARCHAR(255) DEFAULT NULL COMMENT '流水线名称',
    `env` VARCHAR(50) DEFAULT NULL COMMENT '环境',
    `deploy_time` BIGINT NOT NULL COMMENT '发布时间',
    `loc_add` INT DEFAULT 0 COMMENT '新增行数',
    `loc_delete` INT DEFAULT 0 COMMENT '删除行数',
    `loc_modify` INT DEFAULT 0 COMMENT '修改行数',
    `loc_valid` INT DEFAULT 0 COMMENT '有效变更行数',
    `deploy_result` VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/ROLLED_BACK/HOTFIX',
    `is_rollback` TINYINT(1) DEFAULT 0 COMMENT '是否回滚',
    `is_hotfix` TINYINT(1) DEFAULT 0 COMMENT '是否紧急补丁',
    `deployer` VARCHAR(100) DEFAULT NULL COMMENT '发布人',
    `frontend` VARCHAR(255) DEFAULT NULL COMMENT '前端（负责人/开发）',
    `backend` VARCHAR(255) DEFAULT NULL COMMENT '后端（负责人/开发）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` BIGINT NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_story_id` (`story_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_pipeline_id` (`pipeline_id`),
    KEY `idx_deploy_time` (`deploy_time`),
    KEY `idx_story_deploy` (`story_id`, `deploy_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求-流水线变更事实表';


-- 2. 需求表 meego_story_stats 扩展（汇总字段 + 直接取库字段，供看板列表/详情）
--    汇总字段：由定时任务从 requirement_change_stats 等聚合写入
--    直接取库字段：由飞书拉取后写入（code_coverage, frontend_defect_count, backend_defect_count, reopen_rate）；前端/后端缺陷率=前端缺陷数/总缺陷数×100%、后端缺陷数/总缺陷数×100%，总缺陷数=前端+后端，由接口或看板计算
--    若表已有同名列，请跳过对应 ALTER 或使用后续小版本迁移
--    有效变更行数(汇总)不存，由 frontend_loc_changed+backend_loc_changed 实时汇总；千行代码缺陷率=缺陷数/(前端+后端变更行数/1000) 计算
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `frontend_loc_changed` INT DEFAULT 0 COMMENT '前端变更行数';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `backend_loc_changed` INT DEFAULT 0 COMMENT '后端变更行数';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `deploy_total_count` INT DEFAULT 0 COMMENT '总发布次数';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `deploy_failure_count` INT DEFAULT 0 COMMENT '失败次数';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `last_deploy_time` BIGINT DEFAULT NULL COMMENT '最近发布时间';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `change_failure_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '变更失败率(%)';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `avg_write_deviation_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '平均编写工时偏差率(%)';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `avg_exec_deviation_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '平均执行工时偏差率(%)';
-- 直接取库字段（从飞书拉取后写入）
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `code_coverage` DECIMAL(5,2) DEFAULT NULL COMMENT '代码覆盖率(%)，从飞书拉取';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `frontend_defect_count` INT DEFAULT 0 COMMENT '前端缺陷数，从飞书拉取';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `backend_defect_count` INT DEFAULT 0 COMMENT '后端缺陷数，从飞书拉取';
ALTER TABLE spotter_efficiency.`meego_story_stats` ADD COLUMN `reopen_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '重开率(%)，从飞书拉取';
