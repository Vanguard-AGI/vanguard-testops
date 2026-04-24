package io.vanguard.testops.bug.dto.request;

import io.vanguard.testops.bug.dto.response.BugCustomFieldDTO;
import io.vanguard.testops.bug.dto.response.BugFileDTO;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Jan
 */
@Data
public class BugEditRequest implements Serializable {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{bug.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{bug.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "缺陷标题")
    @Size(min = 1, max = 255, message = "{bug.title.length_range}", groups = {Created.class, Updated.class})
    private String title;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{bug.project_id.not_blank}", groups = {Created.class, Updated.class})
    @Size(min = 1, max = 50, message = "{bug.project_id.length_range}", groups = {Created.class, Updated.class})
    private String projectId;

    @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{bug.template_id.not_blank}", groups = {Created.class, Updated.class})
    @Size(min = 1, max = 50, message = "{bug.template_id.length_range}", groups = {Created.class, Updated.class})
    private String templateId;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "缺陷内容")
    private String description;

    @Schema(description = "自定义字段集合")
    private List<BugCustomFieldDTO> customFields;

    @Schema(description = "删除的本地附件集合, {文件ID")
    private List<String> deleteLocalFileIds;

    @Schema(description = "取消关联附件关系ID集合, 关联关系ID")
    private List<String> unLinkRefIds;

    @Schema(description = "关联附件集合, 文件ID")
    private List<String> linkFileIds;

    @Schema(description = "用例ID, 创建缺陷并关联时必填")
    private String caseId;

    @Schema(description = "用例类型, 创建缺陷并关联时必填", allowableValues = {"FUNCTIONAL", "API", "SCENARIO"})
    private String caseType;

    @Schema(description = "测试计划ID,通过测试计划创建的必填")
    private String testPlanId;

    @Schema(description = "测试计划关联的用例ID, 通过测试计划创建的必填，值是关联的关系ID而不是用例ID")
    private String testPlanCaseId;

    @Schema(description = "关联的飞书需求(Story)ID")
    private String feishuStoryId;

    @Schema(description = "缺陷类型(功能性缺陷/性能缺陷/界面缺陷等)")
    private String defectType;

    @Schema(description = "缺陷原因")
    private String defectReason;

    @Schema(description = "所属应用")
    private String appId;

    @Schema(description = "关联影响应用(多选)")
    private String affectedAppIds;

    @Schema(description = "发现阶段")
    private String discoveryPhase;

    @Schema(description = "业务线")
    private String businessLine;

    @Schema(description = "缺陷发现难易度")
    private String discoveryDifficulty;

    @Schema(description = "重开次数")
    private Integer reopenCount;

    @Schema(description = "发现人")
    private String discoverer;

    @Schema(description = "实际时间(毫秒时间戳)")
    private Long actualTime;

    @Schema(description = "复制的附件")
    private List<BugFileDTO> copyFiles;

    @Schema(description = "富文本临时文件ID")
    private List<String> richTextTmpFileIds;
}
