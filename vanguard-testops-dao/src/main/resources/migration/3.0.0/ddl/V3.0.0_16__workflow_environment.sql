-- 为工作流定义表添加环境ID字段，用于保存用例绑定的默认环境
ALTER TABLE workflow_definition 
ADD COLUMN environment_id VARCHAR(50) NULL COMMENT '默认环境ID' 
AFTER global_vars;

-- 添加索引（可选，根据查询需求）
CREATE INDEX idx_environment_id ON workflow_definition (environment_id);
