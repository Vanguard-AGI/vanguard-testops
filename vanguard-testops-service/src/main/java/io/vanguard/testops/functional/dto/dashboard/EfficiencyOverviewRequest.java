package io.vanguard.testops.functional.dto.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 效率仪概览请求DTO
 */
@Data
@Schema(description = "效率仪概览请求")
public class EfficiencyOverviewRequest {

    @Schema(description = "开始日期（格式：YYYY-MM-DD）")
    private String startDate;

    @Schema(description = "结束日期（格式：YYYY-MM-DD）")
    private String endDate;

    @Schema(description = "同比/环比维度。YOY=同比（与上一年度同一时期对比，消除季节/周期波动，反映长期趋势）；MOM=环比（与紧邻上一同统计周期对比，如最近7天 vs 前7天，反映短期变化）。空表示不对比。")
    private String comparisonType;

    @Schema(description = "用户邮箱（个人维度筛选），支持传字符串或数组，如 personal: \"a@b.com\" 或 personal: [\"a@b.com\"]，空表示全部")
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private List<String> personal;

    @Schema(description = "项目搜索：按项目筛选，传项目ID列表，如 projectIds: [\"id1\", \"id2\"]，空表示不按项目筛")
    private List<String> projectIds;

    @Schema(description = "用户ID，用于 metadata_definition 按创建人(create_user)筛选总数；无 personal 时用此字段")
    private String userId;
}
