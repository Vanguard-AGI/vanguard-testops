package io.vanguard.testops.requirementquality.domain;

import lombok.Data;

/**
 * 需求-流水线变更事实表 requirement_change_stats 实体（插入/查询/更新）
 */
@Data
public class RequirementChangeStats {
    private String id;
    private String storyId;
    /** 需求名称（列表展示用，由服务层根据 storyId 从需求库填充，不落库） */
    private String storyName;
    private String projectId;
    /** 项目名称（列表展示用，由服务层根据 projectId 填充，不落库） */
    private String projectName;
    /** 代码仓库名称，表列 repo_name */
    private String repoName;
    private String serviceName;
    /** 其它信息，表列 other_info */
    private String otherInfo;
    private String endpointType;
    private String pipelineId;
    private String pipelineName;
    /** 云效流水线详情链接（解析 other_info BUILD_NUMBER 或用户填写），表列 pipeline_url */
    private String pipelineUrl;
    private String env;
    private Long deployTime;
    private Integer locAdd;
    private Integer locDelete;
    private Integer locModify;
    private Integer locValid;
    private String deployResult;
    private Integer isRollback;
    private Integer isHotfix;
    private String deployer;
    private String frontend;
    private String backend;
    private String remark;
    /** 明细 JSON，如 details 数组序列化 */
    private String details;
    private Long createdAt;
}
