package io.vanguard.testops.functional.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ExtCaseMetricsMapper {
    
    Long countCasesByProject(@Param("projectId") String projectId);
    
    Long countCasesByProjectAndTimeRange(@Param("projectId") String projectId, 
                                         @Param("startTime") Long startTime, 
                                         @Param("endTime") Long endTime);
    
    
    List<Map<String, Object>> getHighFrequencyCases(@Param("projectId") String projectId, 
                                                     @Param("threshold") Integer threshold,
                                                     @Param("startTime") Long startTime, 
                                                     @Param("endTime") Long endTime);
    
    Long countPlanCases(@Param("planId") String planId);
    
    Long countPlanPassedCases(@Param("planId") String planId);
    
    Long countPlanFirstPassCases(@Param("planId") String planId);
    
    List<String> getCaseIdsByProject(@Param("projectId") String projectId);
    
    // 用户维度查询方法
    Long countCasesByUser(@Param("userId") String userId);
    
    Long countCasesByUserAndTimeRange(@Param("userId") String userId, 
                                      @Param("startTime") Long startTime, 
                                      @Param("endTime") Long endTime);
    
    
    List<Map<String, Object>> getHighFrequencyCasesByUser(@Param("userId") String userId, 
                                                           @Param("threshold") Integer threshold,
                                                           @Param("startTime") Long startTime, 
                                                           @Param("endTime") Long endTime);
    
    List<String> getCaseIdsByUser(@Param("userId") String userId);
    
    /**
     * 获取所有创建过用例的用户ID列表
     */
    List<String> getAllUserIds();
    
    /**
     * 获取所有用例ID列表（用于批量计算CS值）
     */
    List<String> getAllCaseIds();
    
    /**
     * 获取指定项目的用例ID列表（用于批量计算CS值）
     */
    List<String> getCaseIdsByProjectForBatch(@Param("projectId") String projectId);
    
    /**
     * 获取所有有用例的项目ID列表（用于定时任务计算指标）
     */
    List<String> getAllProjectIds();
    
    /**
     * 查询 case_metrics_detail 表的总记录数（用于验证数据是否真的保存）
     */
    Long countCaseMetricsDetail();
    
    /**
     * 从 case_change_log 表获取在指定时间范围内被修改的用例ID列表（用于用例变更热度）
     * 排除 change_reason=CASE_COPY；可选限定为「测试计划内修改」或「两库内修改」合并去重
     * @param twoLibraryModuleNames 两库模块名列表（用例模板库、回归用例库），为 null 时不限定范围
     */
    List<String> getModifiedCaseIdsFromChangeLog(@Param("projectId") String projectId,
                                                  @Param("projectIds") List<String> projectIds,
                                                  @Param("userId") String userId,
                                                  @Param("startTime") Long startTime,
                                                  @Param("endTime") Long endTime,
                                                  @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);
    
    /**
     * 从 case_metrics_detail 表获取复用用例ID列表（根据 case_source_type 字段，REUSE/COPY）
     */
    List<String> getReusedCaseIdsFromSourceType(@Param("projectId") String projectId,
                                                 @Param("projectIds") List<String> projectIds,
                                                 @Param("userId") String userId,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime);
    
    /**
     * 获取高频用例ID列表
     */
    List<String> getHighFrequencyCaseIds(@Param("projectId") String projectId,
                                         @Param("threshold") Integer threshold,
                                         @Param("startTime") Long startTime,
                                         @Param("endTime") Long endTime);
    
    List<String> getHighFrequencyCaseIdsByUser(@Param("userId") String userId,
                                               @Param("threshold") Integer threshold,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime);
    
    /**
     * 获取被复用的用例ID列表
     */
    
    /**
     * 根据用例ID列表和时间范围过滤用例ID（从case_metrics_detail表）
     */
    List<String> filterCaseIdsByTimeRange(@Param("caseIds") List<String> caseIds,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime);
    
    /**
     * 根据用例ID列表和时间范围过滤用例ID（从functional_case表的create_time筛选）
     */
    List<String> filterCaseIdsByFunctionalCaseCreateTime(@Param("caseIds") List<String> caseIds,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime);
    
    /**
     * 直接从 case_metrics_detail 表按所有维度一次性筛选用例ID
     * @param projectId 项目ID（null表示全部项目）
     * @param userId 用户ID（null表示全部用户）
     * @param startTime 开始时间（null表示不限）
     * @param endTime 结束时间（null表示不限）
     * @param excludeVersionCaseModule 为 true 时排除「迭代版本管理」模块下的用例（新增率分母口径）
     * @param versionCaseModuleName 排除的模块名，与 excludeVersionCaseModule 配套使用
     * @return 符合条件的用例ID列表
     */
    List<String> getCaseIdsByDimensionAndTime(@Param("projectId") String projectId,
                                              @Param("projectIds") List<String> projectIds,
                                              @Param("userId") String userId,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              @Param("excludeVersionCaseModule") Boolean excludeVersionCaseModule,
                                              @Param("versionCaseModuleName") String versionCaseModuleName);

    /**
     * 两库（用例模板库+回归用例库）下在 endTime 之前已存在的用例ID列表（期初口径）
     */
    List<String> getTwoLibraryCaseIdsBeforeTime(@Param("projectId") String projectId,
                                                @Param("endTime") Long endTime,
                                                @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);

    /**
     * 两库下在 endTime 及之前已存在的用例ID列表（周期内总用例口径）
     */
    List<String> getTwoLibraryCaseIdsUpToTime(@Param("projectId") String projectId,
                                              @Param("projectIds") List<String> projectIds,
                                              @Param("endTime") Long endTime,
                                              @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);

    /**
     * 两库（用例模板库+回归用例库）模块下当前存在的全部用例ID，不按时间筛选，用于指标分母
     */
    List<String> getTwoLibraryCaseIds(@Param("projectId") String projectId,
                                      @Param("projectIds") List<String> projectIds,
                                      @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);

    /**
     * 项目下全部用例ID（不按时间、不按模块），用于用例变更率分母等
     */
    List<String> getTotalCaseIdsByProject(@Param("projectId") String projectId,
                                          @Param("projectIds") List<String> projectIds);

    /**
     * 两库下在 [startTime, endTime] 内新建的用例ID列表（按 create_time，用于有效用例数等）
     */
    List<String> getTwoLibraryNewCaseIdsInPeriod(@Param("projectId") String projectId,
                                                 @Param("projectIds") List<String> projectIds,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime,
                                                 @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);

    /**
     * 用例库中 create_time 在 [startTime, endTime] 内的用例ID列表（用于最近2周新增时，调用方固定传 当前时间-14天～当前时间，不随请求时间范围变化）
     */
    List<String> getNewCaseIdsInTimeRange(@Param("projectId") String projectId,
                                          @Param("projectIds") List<String> projectIds,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime);

    /**
     * 两库+时间范围内「无引用无执行」的新增用例ID（无 test_plan_functional_case 且无 test_plan_case_metrics.exec_count>0），用于用例维度新增率分子
     */
    List<String> getTwoLibraryNewCaseIdsWithoutRefOrExec(@Param("projectId") String projectId,
                                                         @Param("projectIds") List<String> projectIds,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime,
                                                         @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);
    
    /**
     * 获取在指定时间范围内执行过的用例ID列表
     * @param caseIds 用例ID列表（限定范围）
     * @param startTime 开始时间（执行记录的创建时间）
     * @param endTime 结束时间（执行记录的创建时间）
     * @return 在时间范围内执行过的用例ID列表
     */
    List<String> getExecutedCaseIdsInTimeRange(@Param("caseIds") List<String> caseIds,
                                               @Param("startTime") Long startTime,
                                               @Param("endTime") Long endTime);
    
    /**
     * 直接从 case_metrics_detail 表查询用例列表（带分页）
     */
    List<io.vanguard.testops.functional.dto.CaseDetailWithCSDTO> getCaseListByMetricDirect(
            @Param("metricType") String metricType,
            @Param("dimension") String dimension,
            @Param("dimensionValue") String dimensionValue,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的用例总数
     */
    Long countCasesByMetricDirect(
            @Param("metricType") String metricType,
            @Param("dimension") String dimension,
            @Param("dimensionValue") String dimensionValue,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 根据用例ID列表获取CS详情
     */
    List<io.vanguard.testops.functional.domain.CaseMetricsDetail> getCsDetailsByCaseIds(
            @Param("caseIds") List<String> caseIds);
    
    /**
     * 获取执行统计数据
     * 返回字段：first_exec_count, first_pass_count, avg_duration, total_exec_count, manual_exec_count
     */
    Map<String, Object> getExecutionStats(
            @Param("caseIds") List<String> caseIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * 首次通过率与需求质量视图一致：从 case_execution_record 按 (plan_id, case_id) 取时间范围内最早一条执行结果统计。
     * 返回 first_exec_count（有执行的 plan-case 数）、first_pass_count（首次执行结果为 SUCCESS/PASS 的数）。
     */
    Map<String, Object> getFirstPassStatsFromCaseExecutionRecord(
            @Param("caseIds") List<String> caseIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);

    /**
     * 执行热度（仅回归用例库）：按 source_case_id 归父后汇总，仅统计父用例在回归库的 exec_count 与 CS
     * 返回字段：total_exec_count, total_duration, avg_duration, high_freq_cs_total, all_exec_cs_total
     */
    Map<String, Object> getExecutionStatsForRegressionWithParent(
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("regressionModuleName") String regressionModuleName);

    /**
     * 根据计划 id 解析出计划组 id（传组 id 返回组 id，传子计划 id 返回其 group_id）
     */
    String getGroupIdByPlanId(@Param("planId") String planId);

    /**
     * 计划组内用例总数（该组下所有测试计划的用例去重）
     */
    Long countPlanGroupCaseTotal(@Param("groupId") String groupId);

    /**
     * 计划组内「新增来源」用例数（计划组内 case_source_type=NEW 的去重用例数）
     */
    Long countPlanGroupNewCases(@Param("groupId") String groupId);

    /**
     * 计划组内变更次数（case_change_log 中 change_reason!=CASE_COPY 且 case_id 属于计划组的去重数量）
     */
    Long countPlanGroupModifiedCases(@Param("groupId") String groupId,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime);
    
    /**
     * 获取实际执行时间（按测试计划组维度）
     * 汇总周期内测试计划组下所有测试计划的执行时间（从 test_plan_metrics.total_actual_exec_ms）
     * 返回字段：total_actual_exec_ms（毫秒），exec_plan_count（测试计划数量）
     */
    Map<String, Object> getActualExecutionTime(
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("storyIds") List<String> storyIds);

    /**
     * 获取实际编写工时（按测试计划组维度）
     * 获取周期内测试计划组关联的需求的测分编写时间，同一需求只计算一次
     * 返回字段：total_actual_write_ms（毫秒），write_story_count（需求数量）
     */
    Map<String, Object> getActualWriteTime(
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("storyIds") List<String> storyIds);
    
    /**
     * 获取预期编写/执行工时（按测试计划组维度）
     * 周期内创建且关联需求的测试计划组下的用例预期工时（不去重）
     * 返回字段：total_expected_write_ms（毫秒），total_expected_exec_ms（毫秒），case_count（用例数量）
     */
    Map<String, Object> getExpectedTimeByTestPlanGroup(
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("storyIds") List<String> storyIds);
    
    /**
     * 获取预期编写/执行工时按复杂度分级（按测试计划组维度）
     * 周期内创建且关联需求的测试计划组下的用例预期工时（不去重），按复杂度分级
     * 返回列表，每项包含：complexity_level, expected_write_ms, expected_exec_ms, case_count
     */
    List<Map<String, Object>> getExpectedTimeByLevelFromTestPlanGroup(
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("storyIds") List<String> storyIds);
    
    /**
     * 获取用例变更原因分布统计
     */
    List<Map<String, Object>> getChangeReasonDistribution(
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("caseIds") List<String> caseIds);
    
    /**
     * 获取测试用例执行阻塞原因分布统计
     */
    List<Map<String, Object>> getBlockedReasonDistribution(
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("caseIds") List<String> caseIds);
    
    /**
     * 统计已执行的用例数（exec_count > 0）
     */
    Long countExecutedCases(
            @Param("caseIds") List<String> caseIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 查询用例对应的测试计划的总缺陷数
     * 从 test_plan_metrics.total_defect_count 聚合
     */
    Integer getTotalDefectCountForCases(
            @Param("caseIds") List<String> caseIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 查询时间范围内的复用用例详情（case_source_type 为 REUSE/COPY，且来源用例在两库内）
     * 复用用例定义：从两库导入导出的、或直接复制两库的（即 source_case_id 对应源用例在用例模板库/回归用例库）
     *
     * @param projectId 单项目时传
     * @param projectIds 多项目时传
     * @param twoLibraryModuleNames 两库模块名，非空时仅统计「源用例在两库」的复用
     */
    List<io.vanguard.testops.functional.domain.CaseMetricsDetail> getReusedCaseDetailsBySourceType(
            @Param("projectId") String projectId,
            @Param("projectIds") List<String> projectIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("twoLibraryModuleNames") List<String> twoLibraryModuleNames);
    
    /**
     * 统计时间范围内的复用用例数量（根据 case_source_type 字段判断）
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 复用用例数量
     */
    Long countReusedCasesBySourceType(
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 统计时间范围内被修改的用例数（从 case_change_log 表）
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 被修改的用例数（去重）
     */
    Long countModifiedCasesByChangeLog(
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 查询需求列表（支持模糊搜索）
     * @param keyword 搜索关键词（需求ID或需求名称）
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 需求列表（包含关联的测试计划数和用例数统计）
     */
    List<io.vanguard.testops.functional.dto.dashboard.RequirementDTO> getRequirementsList(
            @Param("keyword") String keyword,
            @Param("projectId") String projectId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 根据需求ID列表查询关联的用例ID列表
     * @param storyIds 需求ID列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 关联的用例ID列表（去重）
     */
    List<String> getCaseIdsByRequirements(
            @Param("storyIds") List<String> storyIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 根据变更原因查询用例列表（含需求信息）
     * @param changeReason 变更原因
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用例列表（含关联的需求和测试计划信息）
     */
    List<io.vanguard.testops.functional.dto.dashboard.CaseWithRequirementDTO> getCasesByChangeReason(
            @Param("changeReason") String changeReason,
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
    
    /**
     * 根据阻塞原因查询用例列表（含需求信息）
     * @param blockReason 阻塞原因
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用例列表（含关联的需求和测试计划信息）
     */
    List<io.vanguard.testops.functional.dto.dashboard.CaseWithRequirementDTO> getCasesByBlockReason(
            @Param("blockReason") String blockReason,
            @Param("projectId") String projectId,
            @Param("userId") String userId,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime);
}

