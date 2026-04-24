package io.vanguard.testops.metadata.dto;

import com.google.common.base.CaseFormat;
import io.vanguard.testops.sdk.dto.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class MetadataDefinitionPageRequest extends BaseCondition {

    @Min(value = 1, message = "当前页码必须大于0")
    @Schema(description = "当前页码")
    private int current;

    @Min(value = 1, message = "每页显示条数必须大于0")
    @Schema(description = "每页显示条数（无限制）")
    private int pageSize;

    @Schema(description = "排序字段（model中的字段 : asc/desc）")
    private Map<@Valid @Pattern(regexp = "^[A-Za-z]+$") String, @Valid @NotBlank String> sort;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.project_id.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_definition.project_id.length_range}")
    private String projectId;

    @Schema(description = "模块ID")
    private String moduleId;

    @Schema(description = "类型: HTTP/SQL/DUBBO")
    private String type;

    @Schema(description = "协议类型: HTTP/SQL/DUBBO/SCRIPT/FILE/MQ")
    private String protocol;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "分支名称")
    private String branchName;

    @Schema(description = "只查询最新版本")
    private Boolean latestOnly;

    @Schema(description = "只查询主版本")
    private Boolean masterOnly;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "搜索关键字")
    private String keyword;

    @Schema(description = "创建人")
    private String createUser;

    public String getSortString() {
        if (sort == null || sort.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sort.entrySet()) {
            String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey());
            sb.append(column)
                    .append(StringUtils.SPACE)
                    .append(StringUtils.equalsIgnoreCase(entry.getValue(), "DESC") ? "DESC" : "ASC")
                    .append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }
}
