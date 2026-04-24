package io.vanguard.testops.bug.dto.response;

import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BugDetailDTO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{bug.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{bug.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "业务ID")
    private Integer num;

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

    @Schema(description = "飞书描述富文本HTML（仅飞书缺陷）")
    private String feishuDescriptionHtml;

    @Schema(description = "飞书描述原始doc JSON（仅飞书缺陷，用于图片UUID解析）")
    private String feishuDescriptionDoc;

    @Schema(description = "自定义字段集合")
    private List<BugCustomFieldDTO> customFields;

    @Schema(description = "是否平台默认模板")
    private Boolean platformDefault;

    @Schema(description = "所属平台")
    private String platform;

    @Schema(description = "是否关注")
    private Boolean followFlag;

    @Schema(description = "附件集合")
    private List<BugFileDTO> attachments;

    @Schema(description = "第三方平台缺陷ID")
    private String platformBugId;

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

    @Schema(description = "缺陷状态")
    private String status;

    @Schema(description = "缺陷关联的用例数")
    private long linkCaseCount;

    @Schema(description = "创建人(用户ID)")
    private String createUser;

    @Schema(description = "创建人姓名(展示用)")
    private String createUserName;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新人")
    private String updateUser;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "处理人名称(展示用)")
    private String handleUserName;

    @Schema(description = "状态名称(展示用)")
    private String statusName;

}
