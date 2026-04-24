-- 为 bug 表增加 defect_type 列（若不存在则添加，兼容已含该列的新建库）
SET SESSION innodb_lock_wait_timeout = 7200;

DROP PROCEDURE IF EXISTS proc_bug_add_defect_type;
DELIMITER //
CREATE PROCEDURE proc_bug_add_defect_type()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bug' AND COLUMN_NAME = 'defect_type'
    ) THEN
        ALTER TABLE bug
            ADD COLUMN defect_type VARCHAR(50) NULL COMMENT '缺陷类型(功能性缺陷/性能缺陷/界面缺陷等)' AFTER feishu_story_id;
    END IF;
END //
DELIMITER ;
CALL proc_bug_add_defect_type();
DROP PROCEDURE IF EXISTS proc_bug_add_defect_type;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
