-- 为 workflow_workspace 表添加 icon 和 icon_color 字段 (已在 V3.0.0_14 中添加)
-- ALTER TABLE workflow_workspace 
--     ADD COLUMN icon VARCHAR(20) DEFAULT '📁' COMMENT '图标（emoji）' AFTER description,
--     ADD COLUMN icon_color VARCHAR(50) DEFAULT 'bg-gray-100' COMMENT '图标背景颜色（CSS类名）' AFTER icon;
SELECT 1;
