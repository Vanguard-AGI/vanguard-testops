package io.vanguard.testops.requirementquality.controller;

import io.vanguard.testops.requirementquality.domain.RequirementChangeStats;
import io.vanguard.testops.requirementquality.dto.RequirementQualityDetailDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityFilterOptionsDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityListItemDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityOverviewDTO;
import io.vanguard.testops.requirementquality.request.PipelineRecordCreateRequest;
import io.vanguard.testops.requirementquality.request.PipelineRecordListRequest;
import io.vanguard.testops.requirementquality.request.PipelineRecordUpdateRequest;
import io.vanguard.testops.requirementquality.request.PipelineReportRequest;
import io.vanguard.testops.requirementquality.request.RequirementQualityListRequest;
import io.vanguard.testops.requirementquality.service.PipelineRecordService;
import io.vanguard.testops.requirementquality.service.PipelineReportService;
import io.vanguard.testops.requirementquality.service.RequirementQualityService;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.page.Pager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求质量视图 - 接口层
 * 路径与系分 6.2/6.3/6.4 对齐：列表、概览、详情
 */
@Tag(name = "需求质量视图")
@RestController
@RequestMapping("/metrics/requirement-quality")
public class RequirementQualityController {

    @Resource
    private RequirementQualityService requirementQualityService;

    @Resource
    private PipelineReportService pipelineReportService;

    @Resource
    private PipelineRecordService pipelineRecordService;

    @PostMapping("/pipeline/report")
    @Operation(summary = "流水线白名单上报（运维在云效流水线中通过 curl 上报变更数据）")
    public void pipelineReport(@Validated @RequestBody PipelineReportRequest request) {
        pipelineReportService.report(request);
    }

    @PostMapping("/pipeline/list")
    @Operation(summary = "门禁管理 - 流水线记录列表（分页，支持待补全/项目/服务/时间筛选）")
    public Pager<List<RequirementChangeStats>> pipelineList(@Validated @RequestBody PipelineRecordListRequest request) {
        return pipelineRecordService.pageList(request);
    }

    @PostMapping("/pipeline/update")
    @Operation(summary = "门禁管理 - 运维补全流水线记录（需求ID、项目、环境、发布结果等）")
    public void pipelineUpdate(@Validated @RequestBody PipelineRecordUpdateRequest request) {
        pipelineRecordService.update(request);
    }

    @PostMapping("/pipeline/create")
    @Operation(summary = "发布管理 - 手动创建流水线记录（用户填写后落库）")
    public void pipelineCreate(@Validated @RequestBody PipelineRecordCreateRequest request) {
        pipelineRecordService.create(request);
    }

    @PostMapping("/list")
    @Operation(summary = "需求质量列表（分页）")
    public Pager<List<RequirementQualityListItemDTO>> list(@Validated @RequestBody RequirementQualityListRequest request) {
        return requirementQualityService.pageList(request);
    }

    @PostMapping("/overview")
    @Operation(summary = "概览卡（本期需求数、平均工时偏差等）")
    public RequirementQualityOverviewDTO overview(@Validated @RequestBody RequirementQualityListRequest request) {
        return requirementQualityService.getOverview(request);
    }

    @GetMapping("/detail/{storyId}")
    @Operation(summary = "需求质量详情（当前为概览块，其余模块后续补充）")
    public RequirementQualityDetailDTO detail(@PathVariable String storyId) {
        return requirementQualityService.getDetail(storyId);
    }

    @GetMapping("/filter-options")
    @Operation(summary = "筛选项：项目列表、需求列表、状态（供前端下拉接入）")
    public RequirementQualityFilterOptionsDTO filterOptions() {
        return requirementQualityService.getFilterOptions();
    }

    @GetMapping("/story-search")
    @Operation(summary = "需求库关键词搜索：从完整需求库按 story_id / story_name 模糊匹配，供门禁补全弹窗选择需求")
    public List<OptionDTO> storySearch(@RequestParam(required = false) String keyword) {
        return requirementQualityService.searchStoryByKeyword(keyword);
    }

    @PostMapping("/story-names")
    @Operation(summary = "根据需求 ID 列表批量查需求名称，供缺陷列表等展示")
    public List<OptionDTO> getStoryNamesByIds(@RequestBody List<String> storyIds) {
        return requirementQualityService.getStoryNamesByIds(storyIds);
    }
}
