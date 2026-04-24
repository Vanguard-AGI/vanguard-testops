/*
 * 需求-流水线变更事实表 requirement_change_stats 新增字段 pipeline_url
 * 用于存储云效流水线详情链接：由 other_info 解析 BUILD_NUMBER 得到，或手动创建时用户填写
 * 格式示例：https://flow.aliyun.com/pipelines/{pipelineId}/builds/{buildNumber}
 */
ALTER TABLE spotter_efficiency.requirement_change_stats
    ADD COLUMN pipeline_url VARCHAR(512) DEFAULT NULL COMMENT '云效流水线详情链接（解析 other_info 或用户填写）' AFTER pipeline_name;
