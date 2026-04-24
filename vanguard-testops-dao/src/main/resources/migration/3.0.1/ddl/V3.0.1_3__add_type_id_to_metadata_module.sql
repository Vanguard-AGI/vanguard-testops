-- 为 metadata_module 表添加 type_id 字段 (已在 V3.0.0_14 中添加，此脚本保留为空以满足版本顺序)
-- SET SESSION innodb_lock_wait_timeout = 7200;
-- ALTER TABLE metadata_module 
--     ADD COLUMN type_id VARCHAR(50) NULL COMMENT '根据module_type来存对应业务下第一层ID（如WORKFLOW类型存储workspace_id）',
--     ADD KEY idx_type_id (type_id),
--     ADD KEY idx_project_type (project_id, type_id);
-- SET SESSION innodb_lock_wait_timeout = DEFAULT;
SELECT 1;
