-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- 工作流测试报告权限：下放到项目管理员和项目成员
-- 项目管理员：完整权限（查看、更新、删除）
INSERT INTO user_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'project_admin', 'PROJECT_WORKFLOW_TEST_REPORT:READ');
INSERT INTO user_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'project_admin', 'PROJECT_WORKFLOW_TEST_REPORT:READ+UPDATE');
INSERT INTO user_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'project_admin', 'PROJECT_WORKFLOW_TEST_REPORT:READ+DELETE');

-- 项目成员：仅查看权限（列表、详情、执行记录）
INSERT INTO user_role_permission (id, role_id, permission_id) VALUES (UUID_SHORT(), 'project_member', 'PROJECT_WORKFLOW_TEST_REPORT:READ');

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;
