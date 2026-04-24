-- 修改 workflow_run_step 表的 description 字段类型从 VARCHAR(1000) 改为 TEXT
-- 原因：description 字段可能包含较长的执行描述信息，VARCHAR(1000) 限制导致数据截断错误
-- 修改时间：2026-01-19

ALTER TABLE workflow_run_step 
MODIFY COLUMN description TEXT COMMENT '执行描述/摘要';
