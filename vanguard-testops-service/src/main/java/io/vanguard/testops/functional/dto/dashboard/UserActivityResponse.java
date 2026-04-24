package io.vanguard.testops.functional.dto.dashboard;

import io.vanguard.testops.system.dto.OnlineUserStats;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户活跃度响应DTO
 * series：用户维度 - 一组为用户+bizType(web/plugin/Electron)，一组为用户+module_type，已包含全部用户及明细
 */
@Data
@Schema(description = "用户活跃度响应")
public class UserActivityResponse {

    @Schema(description = "数据系列（用户维度：用户+bizType / 用户+module_type）")
    private List<SeriesGroup> series;

    @Schema(description = "总活跃数（查询时间范围内的总和）")
    private Integer totalActivity;

    @Schema(description = "工具采纳度（活跃用户数/目标用户数、采纳率%）")
    private EfficiencyOverviewResponse.ToolAdoptionRate toolAdoptionRate;

    @Schema(description = "在线用户统计：数量、明细、在线时长（来自 Redis Session）")
    private OnlineUserStats onlineUserStats;

    /**
     * 一组系列（同一维度下的所有用户数据）
     */
    @Data
    @Schema(description = "系列组（如按 bizType / moduleType）")
    public static class SeriesGroup {
        @Schema(description = "维度标识：bizType | moduleType")
        private String dimension;
        @Schema(description = "维度说明")
        private String description;
        @Schema(description = "各用户的系列数据")
        private List<UserSeriesItem> items;
    }

    /**
     * 单个用户在某一维度下的系列数据（按子维度总数，不按日）
     */
    @Data
    @Schema(description = "用户系列项")
    public static class UserSeriesItem {
        @Schema(description = "用户标识（邮箱或用户名）")
        private String user;
        @Schema(description = "该用户总活跃数")
        private Long totalActivity;
        @Schema(description = "按子维度分组的总数，key 为 bizType(Web/Plugin/Electron)、module_type、Case Execution、Automation 等，value 为该维度下的总活跃数")
        private Map<String, Integer> breakdown;
    }
}
