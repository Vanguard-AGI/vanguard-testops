/*
 * ======================================================================================
 * 数据库：spotter_efficiency (质量效能平台)
 * 版本：V3.0.0_13 (Complete)
 * 作用：全量支撑质量指标体系（CS复杂度、UQS质量、工时偏差、复用降本、变更热度）及飞书同步
 * ======================================================================================
 */


-- 1. 用例资产指标详情表 (内部标准系 - 算法维度)
CREATE TABLE IF NOT EXISTS spotter_efficiency.`case_metrics_detail` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `case_id` VARCHAR(50) NOT NULL COMMENT '业务用例ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '归属项目ID',
    `create_user` VARCHAR(50) NOT NULL COMMENT '创建人',
    
    -- [CS复杂度因子]
    `cs_factor_c1` DECIMAL(10,2) DEFAULT 0 COMMENT 'C1:认知/风险(P0/P1) 权重0.5',
    `cs_factor_c2` DECIMAL(10,2) DEFAULT 0 COMMENT 'C2:前置条件数量 权重1.5',
    `cs_factor_c3` DECIMAL(10,2) DEFAULT 0 COMMENT 'C3:数据准备难度 权重4.0',
    `cs_factor_c4` DECIMAL(10,2) DEFAULT 0 COMMENT 'C4:步骤细节数 权重1.0',
    `cs_factor_c5` DECIMAL(10,2) DEFAULT 0 COMMENT 'C5:验证点数量 权重2.0',
    `cs_factor_c6` DECIMAL(10,2) DEFAULT 0 COMMENT 'C6:逻辑分支数量 权重3.0',
    
    -- [CS计算结果]
    `cs_score` DECIMAL(10,2) DEFAULT 0 COMMENT 'CS综合分值',
    `complexity_level` VARCHAR(10) DEFAULT 'L1' COMMENT '复杂度等级: L1/L2/L3/L4',
    
    -- [算法标准工时]
    `case_type` VARCHAR(20) DEFAULT 'MANUAL' COMMENT '用例类型: MANUAL(系数1.0)/AUTO(系数1.5)',
    `env_factor` DECIMAL(3,2) DEFAULT 1.0 COMMENT '环境稳定性因子: 1.0(稳定)/1.5(不稳定-遇到环境阻塞)',
    `algo_expected_write_ms` BIGINT DEFAULT 0 COMMENT '【算法理论】基于CS计算的编写工时 (T) = 基准工时 × case_type因子 × env_factor',
    `algo_expected_exec_ms` BIGINT DEFAULT 0 COMMENT '【算法理论】基于CS计算的执行工时 (E) = 基准工时 × case_type因子 × env_factor',
    
    -- [平台实测工时]
    `platform_actual_write_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】前端埋点统计的真实编写耗时（新建用例）',
    
    -- [复用降本字段]
    `case_source_type` VARCHAR(20) DEFAULT 'NEW' COMMENT '用例来源: NEW(新建)/REUSE(复用自其他用例)/COPY(复制后修改)',
    `modification_cost_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】复用后的修改耗时(毫秒) - 前端 ModificationTracker 埋点',
    `saved_write_ms` BIGINT DEFAULT 0 COMMENT '节约工时(毫秒) = algo_expected_write_ms - modification_cost_ms',
    
    -- [质量评分 UQS]
    `uqs_score` DECIMAL(5,2) DEFAULT 0 COMMENT 'UQS质量评分(0-100)',
    `reuse_count` INT DEFAULT 0 COMMENT '被复用次数',
    `modify_count` INT DEFAULT 0 COMMENT '核心内容变更次数',
    
    `create_time` BIGINT NOT NULL,
    `update_time` BIGINT NOT NULL,
    `last_calc_time` BIGINT NOT NULL COMMENT '指标最后重算时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_case_id` (`case_id`),
    KEY `idx_level` (`complexity_level`),
    KEY `idx_uqs` (`uqs_score`),
    KEY `idx_case_source_type` (`case_source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例资产指标表(存储内部算法标准)';


-- 2. 测试计划执行快照表 (过程监控系)
CREATE TABLE IF NOT EXISTS spotter_efficiency.`test_plan_case_metrics` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `test_plan_id` VARCHAR(50) NOT NULL COMMENT '测试计划ID',
    `case_id` VARCHAR(50) NOT NULL COMMENT '用例ID',
    `project_id` VARCHAR(50) NOT NULL,
    
    -- [复用降本指标 - 快照值]
    `case_source_type` VARCHAR(20) NOT NULL COMMENT '来源快照: NEW(新建)/REUSE(复用)/MODIFY(复用修改)',
    `modification_cost_ms` BIGINT DEFAULT 0 COMMENT '【快照】复用后的修改耗时 - 从 case_metrics_detail 读取',
    `saved_write_ms` BIGINT DEFAULT 0 COMMENT '【快照】节约工时 - 从 case_metrics_detail 读取',
    
    -- [执行关键指标]
    `exec_count` INT DEFAULT 0 COMMENT '本计划内执行次数',
    `first_exec_time` BIGINT NULL COMMENT '首次执行时间',
    `first_exec_result` VARCHAR(20) NULL COMMENT '首次执行结果(关键): PASS/FAIL/BLOCK',
    `last_exec_result` VARCHAR(20) NULL COMMENT '最终结果',
    
    -- [UQS计算因子]
    `is_blocked_run` TINYINT(1) DEFAULT 0 COMMENT '是否阻塞(影响可执行率)',
    `block_reason` VARCHAR(50) NULL COMMENT '阻塞原因: ENVIRONMENT(环境因素)/RESOURCE_SHORTAGE(资源不足阻塞)/PREREQUISITE_DEPENDENCY(前置依赖阻塞)/REQUIREMENT_UNCLEAR(需求方案不明确阻塞)/TECHNICAL_DIFFICULTY(技术难点阻塞)/PROCESS_COMMUNICATION(流程沟通阻塞)',
    
    -- [执行耗时追踪 - ExecutionTracker埋点]
    `actual_exec_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】累积执行耗时(毫秒) - ExecutionTracker.executionTime',
    `actual_reading_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】累积阅读耗时(毫秒) - ExecutionTracker.readingTime',
    `total_time_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】总耗时(毫秒) = actual_exec_ms + actual_reading_ms',
    `is_batch_fill` TINYINT DEFAULT 0 COMMENT '是否批量填表 - ExecutionTracker.isBatch',
    `focus_out_count` INT DEFAULT 0 COMMENT '切出次数(窗口失焦次数) - ExecutionTracker.focusOutCount',
    `filtered_time_ms` BIGINT DEFAULT 0 COMMENT '被过滤的无效时长(毫秒) - ExecutionTracker.filteredTime',
    
    -- [快照]
    `snapshot_cs_score` DECIMAL(10,2) DEFAULT 0,
    `snapshot_level` VARCHAR(10) NULL,
    
    `create_time` BIGINT NOT NULL,
    `update_time` BIGINT NOT NULL,
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_case` (`test_plan_id`, `case_id`),
    KEY `idx_first_res` (`first_exec_result`),
    KEY `idx_source` (`case_source_type`),
    KEY `idx_block_reason` (`block_reason`),
    KEY `idx_actual_exec_ms` (`actual_exec_ms`),
    KEY `idx_is_batch_fill` (`is_batch_fill`),
    KEY `idx_total_time` (`total_time_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试计划执行快照表';


-- 3. 测试计划聚合表 (结果验证系)
CREATE TABLE IF NOT EXISTS spotter_efficiency.`test_plan_metrics` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `test_plan_id` VARCHAR(50) NOT NULL,
    `project_id` VARCHAR(50) NOT NULL,
    
    -- [环境稳定性]
    `env_instability_factor` DECIMAL(3,2) DEFAULT 1.0 COMMENT '环境因子: 稳定1.0 / 不稳定1.5',
    
    -- [计划级质量指标]
    `total_cases` INT DEFAULT 0 COMMENT '总用例数',
    `first_pass_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '首次通过率%',
    `avg_cs_score` DECIMAL(10,2) DEFAULT 0 COMMENT '平均复杂度CS',
    
    -- [算法工时聚合]
    `total_algo_write_ms` BIGINT DEFAULT 0 COMMENT '【算法理论】计划总编写工时',
    `total_algo_exec_ms` BIGINT DEFAULT 0 COMMENT '【算法理论】计划总执行工时',
    `reuse_saving_hours` DECIMAL(10,2) DEFAULT 0 COMMENT '复用总节约工时(小时)',
    
    -- [实测工时聚合 - ExecutionTracker埋点]
    `total_actual_exec_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】计划总执行工时(毫秒) - 所有用例actual_exec_ms累加',
    `total_actual_reading_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】计划总阅读工时(毫秒) - 所有用例actual_reading_ms累加',
    `total_actual_time_ms` BIGINT DEFAULT 0 COMMENT '【平台实测】计划总耗时(毫秒) = total_actual_exec_ms + total_actual_reading_ms',
    `avg_exec_deviation_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '平均执行时长偏差率(%) - [(实际-预期)/预期]×100%',
    `complexity_variance` DECIMAL(10,2) DEFAULT 0 COMMENT '用例复杂度方差 - 衡量复杂度分布离散程度',
    
    -- [编写时长偏差率]
    `write_deviation_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '编写时长偏差率(%) - [(实际编写时长-预期编写时长)/预期编写时长]×100%',
    
    -- [缺陷统计]
    `total_defect_count` INT DEFAULT 0 COMMENT '总缺陷数 - 从飞书需求 meego_story_stats.defect_count 读取',
    
    `start_time` BIGINT NULL,
    `end_time` BIGINT NULL,
    `last_calc_time` BIGINT NOT NULL,
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan` (`test_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试计划聚合指标表';


-- 4. 用例变更审计表 (热度分析)
CREATE TABLE IF NOT EXISTS spotter_efficiency.`case_change_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `case_id` VARCHAR(50) NOT NULL,
    `project_id` VARCHAR(50) NOT NULL,
    
    -- [热度统计因子]
    `change_reason` VARCHAR(50) NOT NULL COMMENT '变更原因: REQUIREMENT_TEMP(需求临时变更)/REQUIREMENT_ITERATION(需求迭代变更)/CASE_DESIGN(用例设计变更)/CASE_MAINTENANCE(历史用例维护)/TECH_SOLUTION(技术方案适配)/RESOURCE_ADJUSTMENT(资源配置调整)/EXTERNAL_DEPENDENCY(外部依赖变更)/COMPLIANCE_POLICY(合规政策要求)',
    `change_area` VARCHAR(50) NOT NULL COMMENT '变更区域: TITLE/STEP/EXPECTED',
    
    `operator` VARCHAR(50) NOT NULL,
    `create_time` BIGINT NOT NULL COMMENT '变更时间',
    
    KEY `idx_heat_stats` (`case_id`, `change_reason`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例变更审计表';


-- 5. 飞书Meego需求统计表
CREATE TABLE IF NOT EXISTS spotter_efficiency.`meego_story_stats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `story_id` VARCHAR(100) NOT NULL COMMENT '需求唯一标识 (Story ID)',
    `story_name` VARCHAR(255) COMMENT '需求名称 (Story Name)',
    `defect_count` INT DEFAULT 0 COMMENT '该需求关联的缺陷数量',
    `test_analysis_time` DECIMAL(10,2) DEFAULT 0.00 COMMENT '需求的测分编写预期时间 (人天)',
    `updated_at` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_story_id` (`story_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书Meego需求统计表';


-- 6. 用例执行记录表 (用于统计高频用例和执行时长)
CREATE TABLE IF NOT EXISTS spotter_efficiency.`case_execution_record` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `case_id` VARCHAR(50) NOT NULL COMMENT '用例ID',
    `project_id` VARCHAR(50) NOT NULL COMMENT '项目ID',
    `plan_id` VARCHAR(50) COMMENT '测试计划ID',
    `executor_id` VARCHAR(50) NOT NULL COMMENT '执行人ID',
    `exec_result` VARCHAR(64) NOT NULL COMMENT '执行结果：PASS-通过, FAIL-失败, BLOCKED-阻塞, SKIP-跳过',
    `exec_duration` BIGINT DEFAULT 0 COMMENT '执行时长（毫秒）',
    `exec_start_time` BIGINT COMMENT '执行开始时间',
    `exec_end_time` BIGINT COMMENT '执行结束时间',
    `is_first_exec` TINYINT(1) DEFAULT 0 COMMENT '是否首次执行：0-否, 1-是',
    `case_cs_score` DECIMAL(10,2) COMMENT '执行时的用例CS分值',
    `create_time` BIGINT NOT NULL COMMENT '创建时间',
    `update_time` BIGINT NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_case_id` (`case_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_executor_id` (`executor_id`),
    KEY `idx_exec_result` (`exec_result`),
    KEY `idx_exec_start_time` (`exec_start_time`),
    KEY `idx_exec_end_time` (`exec_end_time`),
    KEY `idx_create_time` (`create_time` DESC),
    KEY `idx_case_id_time` (`case_id`, `exec_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用例执行记录表（用于统计高频用例和执行时长）';
