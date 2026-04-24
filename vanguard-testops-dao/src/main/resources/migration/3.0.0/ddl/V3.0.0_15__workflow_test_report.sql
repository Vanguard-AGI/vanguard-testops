-- ==================== 工作流测试报告表 ====================
-- 用于聚合多个工作流执行结果，生成综合测试报告
CREATE TABLE workflow_test_report (
    report_id                VARCHAR(50)  NOT NULL COMMENT '报告ID',
    project_id               VARCHAR(50)  NOT NULL COMMENT '项目ID',
    report_name              VARCHAR(255) NOT NULL COMMENT '报告名称',
    
    -- 报告类型和标签
    report_type              VARCHAR(20)  DEFAULT 'MANUAL' COMMENT '报告类型: MANUAL(手动生成)/AUTO(自动生成)/SCHEDULE(定时生成)',
    tags                     JSON                   COMMENT '标签列表，如：["电商", "核心功能", "回归测试"]',
    
    -- 执行信息
    executor                 VARCHAR(50)  NOT NULL COMMENT '执行人/创建人',
    trigger_type             VARCHAR(20)  DEFAULT 'MANUAL' COMMENT '触发类型: MANUAL(手动)/SCHEDULE(定时)/API(接口触发)',
    
    -- 状态信息
    status                   VARCHAR(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '报告状态: RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)/CANCELLED(已取消)',
    
    -- 时间信息
    start_time               BIGINT                COMMENT '开始时间（毫秒）',
    end_time                 BIGINT                COMMENT '结束时间（毫秒）',
    duration_ms              BIGINT                COMMENT '报告生成耗时（毫秒）：从reportId生成到所有workflow完成的时间差',
    execution_duration_ms    BIGINT                COMMENT '执行时长（毫秒）：所有workflow执行耗时的总和',
    
    -- 统计信息（聚合多个工作流的执行结果）
    total_workflows          INT          DEFAULT 0 COMMENT '包含的工作流数量',
    total_tests              INT          DEFAULT 0 COMMENT '总测试数（所有工作流的步骤总数）',
    success_tests            INT          DEFAULT 0 COMMENT '成功测试数',
    failed_tests             INT          DEFAULT 0 COMMENT '失败测试数',
    skipped_tests            INT          DEFAULT 0 COMMENT '跳过测试数',
    pending_tests            INT          DEFAULT 0 COMMENT '待执行测试数',
    
    -- 成功率
    success_rate             DECIMAL(5, 2)          COMMENT '成功率（百分比，如：92.30）',
    
    -- 平均执行时长（秒）
    avg_duration_seconds     INT                    COMMENT '平均执行时长（秒）',
    
    -- 报告摘要和详情
    summary                  TEXT                   COMMENT '报告摘要/描述',
    result_summary           JSON                   COMMENT '详细结果摘要（JSON格式，包含各工作流的执行情况）',
    
    -- 环境信息
    environment_id           VARCHAR(50)            COMMENT '执行环境ID',
    environment_name         VARCHAR(255)           COMMENT '执行环境名称（快照）',
    
    -- 报告文件（可选，用于导出）
    report_file_id           VARCHAR(50)            COMMENT '报告文件ID（关联 metadata_file_resource）',
    
    -- 元数据
    create_time              BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
    create_user              VARCHAR(50)  NOT NULL COMMENT '创建人',
    update_time              BIGINT       NOT NULL COMMENT '最后更新时间（毫秒）',
    update_user              VARCHAR(50)           COMMENT '最后更新人',
    deleted_time             DATETIME     NULL DEFAULT NULL COMMENT '删除时间（软删除，NULL表示未删除）',
    deleted_by               VARCHAR(50)            COMMENT '删除人',
    
    PRIMARY KEY (report_id),
    KEY idx_project (project_id),
    KEY idx_executor (executor),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_start_time (start_time),
    KEY idx_deleted_time (deleted_time)
) COMMENT='工作流测试报告';

-- ==================== 在 workflow_run 表中添加 report_id 字段 ====================
-- 用于关联工作流运行记录到测试报告
ALTER TABLE workflow_run ADD COLUMN report_id VARCHAR(50) COMMENT '关联的测试报告ID（一个运行记录只能属于一个报告）';
ALTER TABLE workflow_run ADD KEY idx_report_id (report_id);

-- 外部插件同步节点数据表（HTTP/SQL类型）
CREATE TABLE workflow_plugin_sync_node (
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID' PRIMARY KEY,
    email VARCHAR(255) NOT NULL COMMENT '用户邮箱',
    node_type VARCHAR(20) NOT NULL COMMENT '节点类型: HTTP/SQL',
    endpoint_data JSON NOT NULL COMMENT '节点数据（JSON格式，包含endpoint的完整信息）',
    create_time BIGINT NOT NULL COMMENT '创建时间（毫秒）',
    update_time BIGINT NOT NULL COMMENT '更新时间（毫秒）',
    deleted_time DATETIME NULL COMMENT '删除时间（软删除，NULL表示未删除）'
) COMMENT '外部插件同步节点数据表';

-- 创建索引
CREATE INDEX idx_email ON workflow_plugin_sync_node (email);
CREATE INDEX idx_type ON workflow_plugin_sync_node (node_type);
CREATE INDEX idx_deleted ON workflow_plugin_sync_node (deleted_time);
CREATE INDEX idx_create_time ON workflow_plugin_sync_node (create_time);