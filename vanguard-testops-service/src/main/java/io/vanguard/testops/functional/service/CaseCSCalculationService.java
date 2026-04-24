package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.*;
import io.vanguard.testops.functional.mapper.*;
import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CS(Complexity Score) 复杂分计算服务
 * 实现基于7因子的用例复杂度评分算法
 */
@Slf4j
@Service
public class CaseCSCalculationService {

    @Resource
    private FunctionalCaseMapper functionalCaseMapper;

    @Resource
    private FunctionalCaseBlobMapper functionalCaseBlobMapper;

    @Resource
    private FunctionalCaseCustomFieldMapper functionalCaseCustomFieldMapper;
    
    @Resource
    private FunctionalCaseCustomFieldService functionalCaseCustomFieldService;
    
    @Resource
    private ExtCaseMetricsMapper extCaseMetricsMapper;
    
    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;

    private static final double W1 = 0.5;
    private static final double W2 = 1.5;
    private static final double W3 = 4.0;
    private static final double W4 = 1.0;
    private static final double W5 = 2.0;
    private static final double W6 = 3.0;

    private static final String[] COMPLEX_DATA_KEYWORDS = {"SQL", "数据库", "造数", "外部系统", "DATABASE", "INSERT", "UPDATE"};
    private static final String[] BRANCH_KEYWORDS = {"如果.*则", "如果.*否则", "IF.*THEN", "IF.*ELSE", "假如", "当.*时", "WHEN", "CASE"};

    /**
     * 计算单个用例的CS分值（内部使用中间对象）
     */
    private CSCalculationResult calculateCaseCSInternal(String caseId) {
        FunctionalCase functionalCase = functionalCaseMapper.selectByPrimaryKey(caseId);
        if (functionalCase == null) {
            throw new RuntimeException("用例不存在: " + caseId);
        }

        FunctionalCaseBlob caseBlob = functionalCaseBlobMapper.selectByPrimaryKey(caseId);
        if (caseBlob == null) {
            throw new RuntimeException("用例详情不存在: " + caseId);
        }

        // 计算各个因子
        BigDecimal c1 = calculateRiskLevelScore(caseId);        // C1: 认知/风险 (P0/P1)
        BigDecimal c2 = countPrerequisites(caseBlob);            // C2: 前置条件数量
        BigDecimal c3 = detectComplexDataPreparation(caseBlob); // C3: 数据准备难度
        BigDecimal c4 = countOperationSteps(caseBlob);           // C4: 步骤细节数
        BigDecimal c5 = countValidationPoints(caseBlob);         // C5: 验证点数量
        BigDecimal c6 = countLogicalBranches(caseBlob);          // C6: 逻辑分支数量

        // 计算总CS分值 = Σ(Ci × Wi)
        BigDecimal cognitiveScore = c1.multiply(BigDecimal.valueOf(W1));
        BigDecimal preconditionScore = c2.multiply(BigDecimal.valueOf(W2))
                .add(c3.multiply(BigDecimal.valueOf(W3)));
        BigDecimal stepDetailScore = c4.multiply(BigDecimal.valueOf(W4))
                .add(c5.multiply(BigDecimal.valueOf(W5)))
                .add(c6.multiply(BigDecimal.valueOf(W6)));

        BigDecimal totalCS = cognitiveScore.add(preconditionScore).add(stepDetailScore);

        CSCalculationResult result = new CSCalculationResult();
        result.setCaseId(caseId);
        result.setProjectId(functionalCase.getProjectId());
        result.setCreateUser(functionalCase.getCreateUser());
        result.setCreateTime(functionalCase.getCreateTime());
        result.setCsScore(totalCS);
        result.setCognitiveScore(cognitiveScore);
        result.setPreconditionScore(preconditionScore);
        result.setStepDetailScore(stepDetailScore);
        result.setCsFactorC1(c1);
        result.setCsFactorC2(c2);
        result.setCsFactorC3(c3);
        result.setCsFactorC4(c4);
        result.setCsFactorC5(c5);
        result.setCsFactorC6(c6);
        result.setCsFactorC7(BigDecimal.ZERO); // C7在新表中不使用
        result.setSourceCaseId(functionalCase.getSourceCaseId());
        result.setReuseType(functionalCase.getReuseType());

        return result;
    }
    
    /**
     * 批量计算用例的CS分值（使用预查询的数据，性能优化）
     */
    private CSCalculationResult calculateCaseCSInternalBatch(
            FunctionalCase functionalCase,
            FunctionalCaseBlob caseBlob,
            Map<String, List<FunctionalCaseCustomField>> customFieldMap) {
        String caseId = functionalCase.getId();
        
        if (functionalCase == null) {
            throw new RuntimeException("用例不存在: " + caseId);
        }
        if (caseBlob == null) {
            throw new RuntimeException("用例详情不存在: " + caseId);
        }

        // 计算各个因子（使用预查询的数据）
        BigDecimal c1 = calculateRiskLevelScoreFromMap(caseId, customFieldMap); // C1: 认知/风险 (P0/P1)
        BigDecimal c2 = countPrerequisites(caseBlob);            // C2: 前置条件数量
        BigDecimal c3 = detectComplexDataPreparation(caseBlob); // C3: 数据准备难度
        BigDecimal c4 = countOperationSteps(caseBlob);           // C4: 步骤细节数
        BigDecimal c5 = countValidationPoints(caseBlob);         // C5: 验证点数量
        BigDecimal c6 = countLogicalBranches(caseBlob);          // C6: 逻辑分支数量

        // 计算总CS分值 = Σ(Ci × Wi)
        BigDecimal cognitiveScore = c1.multiply(BigDecimal.valueOf(W1));
        BigDecimal preconditionScore = c2.multiply(BigDecimal.valueOf(W2))
                .add(c3.multiply(BigDecimal.valueOf(W3)));
        BigDecimal stepDetailScore = c4.multiply(BigDecimal.valueOf(W4))
                .add(c5.multiply(BigDecimal.valueOf(W5)))
                .add(c6.multiply(BigDecimal.valueOf(W6)));

        BigDecimal totalCS = cognitiveScore.add(preconditionScore).add(stepDetailScore);

        CSCalculationResult result = new CSCalculationResult();
        result.setCaseId(caseId);
        result.setProjectId(functionalCase.getProjectId());
        result.setCreateUser(functionalCase.getCreateUser());
        result.setCreateTime(functionalCase.getCreateTime());
        result.setCsScore(totalCS);
        result.setCognitiveScore(cognitiveScore);
        result.setPreconditionScore(preconditionScore);
        result.setStepDetailScore(stepDetailScore);
        result.setCsFactorC1(c1);
        result.setCsFactorC2(c2);
        result.setCsFactorC3(c3);
        result.setCsFactorC4(c4);
        result.setCsFactorC5(c5);
        result.setCsFactorC6(c6);
        result.setCsFactorC7(BigDecimal.ZERO); // C7在新表中不使用
        result.setSourceCaseId(functionalCase.getSourceCaseId());
        result.setReuseType(functionalCase.getReuseType());

        return result;
    }

    /**
     * 计算认知复杂度（C1 × W1）
     */
    private BigDecimal calculateCognitiveScore(FunctionalCase functionalCase) {
        BigDecimal c1 = calculateRiskLevelScore(functionalCase.getId());
        return c1.multiply(BigDecimal.valueOf(W1));
    }

    /**
     * 计算前置条件复杂度 = (C2 × W2) + (C3 × W3)
     */
    private BigDecimal calculatePreconditionScore(FunctionalCaseBlob caseBlob) {
        BigDecimal c2 = countPrerequisites(caseBlob);
        BigDecimal c3 = detectComplexDataPreparation(caseBlob);

        return c2.multiply(BigDecimal.valueOf(W2))
                .add(c3.multiply(BigDecimal.valueOf(W3)));
    }

    /**
     * 计算步骤细节复杂度 = (C4 × W4) + (C5 × W5) + (C6 × W6)
     */
    private BigDecimal calculateStepDetailScore(FunctionalCaseBlob caseBlob) {
        BigDecimal c4 = countOperationSteps(caseBlob);
        BigDecimal c5 = countValidationPoints(caseBlob);
        BigDecimal c6 = countLogicalBranches(caseBlob);

        return c4.multiply(BigDecimal.valueOf(W4))
                .add(c5.multiply(BigDecimal.valueOf(W5)))
                .add(c6.multiply(BigDecimal.valueOf(W6)));
    }

    /**
     * C1: 风险等级得分（检测P0/P1标签）
     */
    private BigDecimal calculateRiskLevelScore(String caseId) {
        try {
            FunctionalCaseCustomFieldExample example = new FunctionalCaseCustomFieldExample();
            example.createCriteria().andCaseIdEqualTo(caseId);
            List<FunctionalCaseCustomField> customFields = functionalCaseCustomFieldMapper.selectByExample(example);

            for (FunctionalCaseCustomField field : customFields) {
                String value = field.getValue();
                if (StringUtils.isNotBlank(value) && (value.contains("P0") || value.contains("P1"))) {
                    return BigDecimal.ONE;
                }
            }
        } catch (Exception e) {
            log.warn("获取用例风险等级失败: {}", caseId, e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * C1: 风险等级得分（从预查询的自定义字段数据中查找）
     */
    private BigDecimal calculateRiskLevelScoreFromMap(String caseId, Map<String, List<FunctionalCaseCustomField>> customFieldMap) {
        try {
            List<FunctionalCaseCustomField> customFields = customFieldMap.get(caseId);
            if (customFields != null) {
                for (FunctionalCaseCustomField field : customFields) {
                    String value = field.getValue();
                    if (StringUtils.isNotBlank(value) && (value.contains("P0") || value.contains("P1"))) {
                        return BigDecimal.ONE;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取用例风险等级失败: {}", caseId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * C2: 前置条件数量
     */
    private BigDecimal countPrerequisites(FunctionalCaseBlob caseBlob) {
        if (caseBlob.getPrerequisite() == null) {
            return BigDecimal.ZERO;
        }

        String prerequisiteText = new String(caseBlob.getPrerequisite(), StandardCharsets.UTF_8);

        try {
            List<Map<String, Object>> prerequisiteList = JSON.parseArray(prerequisiteText);
            return BigDecimal.valueOf(Math.max(prerequisiteList.size(), 0));
        } catch (Exception e) {
            String[] lines = prerequisiteText.split("\n");
            long count = Arrays.stream(lines).filter(line -> !line.trim().isEmpty()).count();
            return BigDecimal.valueOf(Math.max(count, 0));
        }
    }

    /**
     * C3: 复杂数据准备检测（正则匹配关键词）
     */
    private BigDecimal detectComplexDataPreparation(FunctionalCaseBlob caseBlob) {
        if (caseBlob.getPrerequisite() == null) {
            return BigDecimal.ZERO;
        }

        String prerequisiteText = new String(caseBlob.getPrerequisite(), StandardCharsets.UTF_8).toUpperCase();

        for (String keyword : COMPLEX_DATA_KEYWORDS) {
            if (prerequisiteText.contains(keyword)) {
                return BigDecimal.ONE;
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * C4: 操作步骤数
     */
    private BigDecimal countOperationSteps(FunctionalCaseBlob caseBlob) {
        if (caseBlob.getSteps() == null) {
            return BigDecimal.ONE;
        }

        String stepsText = new String(caseBlob.getSteps(), StandardCharsets.UTF_8);

        try {
            List<Map<String, Object>> stepsList = JSON.parseArray(stepsText);
            return BigDecimal.valueOf(Math.max(stepsList.size(), 1));
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    /**
     * C5: 验证点数
     */
    private BigDecimal countValidationPoints(FunctionalCaseBlob caseBlob) {
        int validationCount = 0;

        if (caseBlob.getSteps() != null) {
            String stepsText = new String(caseBlob.getSteps(), StandardCharsets.UTF_8);
            try {
                List<Map<String, Object>> stepsList = JSON.parseArray(stepsText);
                for (Map<String, Object> step : stepsList) {
                    if (step.containsKey("expected") && StringUtils.isNotBlank((String) step.get("expected"))) {
                        validationCount++;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (validationCount == 0 && caseBlob.getExpectedResult() != null) {
            String expectedText = new String(caseBlob.getExpectedResult(), StandardCharsets.UTF_8);
            String[] lines = expectedText.split("\n");
            validationCount = (int) Arrays.stream(lines)
                    .filter(line -> !line.trim().isEmpty())
                    .count();
        }

        return BigDecimal.valueOf(Math.max(validationCount, 1));
    }

    /**
     * C6: 逻辑分支数量
     */
    private BigDecimal countLogicalBranches(FunctionalCaseBlob caseBlob) {
        int branchCount = 0;

        String[] fieldsToCheck = {
                caseBlob.getSteps() != null ? new String(caseBlob.getSteps(), StandardCharsets.UTF_8) : "",
                caseBlob.getExpectedResult() != null ? new String(caseBlob.getExpectedResult(), StandardCharsets.UTF_8) : "",
                caseBlob.getPrerequisite() != null ? new String(caseBlob.getPrerequisite(), StandardCharsets.UTF_8) : "",
                caseBlob.getTextDescription() != null ? new String(caseBlob.getTextDescription(), StandardCharsets.UTF_8) : ""
        };

        for (String text : fieldsToCheck) {
            for (String keyword : BRANCH_KEYWORDS) {
                Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    branchCount++;
                }
            }
        }

        return BigDecimal.valueOf(branchCount);
    }

    /**
     * 获取或计算单个用例的CS详情
     */
    public CaseMetricsDetail getOrCalculateCSDetail(String caseId) {
        CaseMetricsDetail existing = caseMetricsDetailMapper.selectByCaseId(caseId);
        if (existing != null) {
            return existing;
        }
        // 如果不存在，计算并保存
        CSCalculationResult csResult = calculateCaseCSInternal(caseId);
        CaseMetricsDetail detail = convertToDetail(csResult);
        caseMetricsDetailMapper.insert(detail);
        return detail;
    }

    /**
     * 重新计算指定用例的CS值（强制更新）
     * 注意：会保留现有的 platform_actual_write_ms、uqs_score、reuse_count、modify_count、
     *      case_source_type、modification_cost_ms、saved_write_ms 等埋点数据
     */
    public void recalculateCaseCS(String caseId) {
        CSCalculationResult csResult = calculateCaseCSInternal(caseId);
        CaseMetricsDetail detail = convertToDetail(csResult);
        CaseMetricsDetail existing = caseMetricsDetailMapper.selectByCaseId(caseId);
        if (existing == null) {
            caseMetricsDetailMapper.insert(detail);
        } else {
            // 保留现有的埋点数据和统计字段
            detail.setId(existing.getId());
            if (existing.getPlatformActualWriteMs() != null && existing.getPlatformActualWriteMs() > 0) {
                detail.setPlatformActualWriteMs(existing.getPlatformActualWriteMs());
            }
            if (existing.getUqsScore() != null) {
                detail.setUqsScore(existing.getUqsScore());
            }
            if (existing.getReuseCount() != null) {
                detail.setReuseCount(existing.getReuseCount());
            }
            if (existing.getModifyCount() != null) {
                detail.setModifyCount(existing.getModifyCount());
            }
            // 保留环境因子（如果已因环境阻塞调整为1.5，重算时保留）
            if (existing.getEnvFactor() != null) {
                detail.setEnvFactor(existing.getEnvFactor());
                
                // 如果环境因子不是1.0，需要重新应用环境因子到理论工时
                if (existing.getEnvFactor().compareTo(BigDecimal.ONE) != 0) {
                    long adjustedWriteMs = (long) (detail.getAlgoExpectedWriteMs() * existing.getEnvFactor().doubleValue());
                    long adjustedExecMs = (long) (detail.getAlgoExpectedExecMs() * existing.getEnvFactor().doubleValue());
                    detail.setAlgoExpectedWriteMs(adjustedWriteMs);
                    detail.setAlgoExpectedExecMs(adjustedExecMs);
                }
            }
            
            // 保留复用相关字段
            if (existing.getCaseSourceType() != null) {
                detail.setCaseSourceType(existing.getCaseSourceType());
            }
            if (existing.getModificationCostMs() != null) {
                detail.setModificationCostMs(existing.getModificationCostMs());
                
                // CS值重新计算后，同步更新节约工时（因为 algo_expected_write_ms 可能变化）
                if ("REUSE".equals(existing.getCaseSourceType()) && existing.getModificationCostMs() > 0) {
                    long savedMs = detail.getAlgoExpectedWriteMs() - existing.getModificationCostMs();
                    detail.setSavedWriteMs(Math.max(0, savedMs));
                } else {
                    detail.setSavedWriteMs(existing.getSavedWriteMs() != null ? existing.getSavedWriteMs() : 0L);
                }
            } else {
                detail.setSavedWriteMs(0L);
            }
            // 保留创建时间
            if (existing.getCreateTime() != null) {
                detail.setCreateTime(existing.getCreateTime());
            }
            // source_case_id 使用本次从 functional_case 带入的值（detail 已由 convertToDetail 设置）
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
        }
    }

    /**
     * 批量计算并写入 case_metrics_detail 表（公开接口）
     * @param projectId 项目ID，null 表示全部项目
     * @param forceRecalculate 是否强制重新计算
     * @return 写入成功的记录数
     */
    /**
     * 批量计算并写入 case_metrics_detail（分批批量处理，性能优化）
     * 每批处理2000条数据，批量查询和批量写入，大幅减少数据库交互次数
     * 异步执行，不阻塞主线程
     */
    @Async("csCalculationExecutor")
    public void batchCalculateMetricsDetailAsync(String projectId, boolean forceRecalculate) {
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        final int BATCH_SIZE = 2000; // 每批处理2000条
        
        List<String> caseIds = (projectId != null) ?
                extCaseMetricsMapper.getCaseIdsByProjectForBatch(projectId) :
                extCaseMetricsMapper.getAllCaseIds();
        
        if (caseIds == null || caseIds.isEmpty()) {
            log.warn("未找到需要计算的用例");
            return;
        }
        
        // 分批处理
        for (int i = 0; i < caseIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, caseIds.size());
            List<String> batchCaseIds = caseIds.subList(i, endIndex);
            try {
                BatchProcessResult result = processBatch(batchCaseIds, forceRecalculate);
                successCount += result.successCount;
                skipCount += result.skipCount;
                failCount += result.failCount;
            } catch (Exception e) {
                log.error("处理第 {}/{} 批时发生异常", (i / BATCH_SIZE + 1), (caseIds.size() + BATCH_SIZE - 1) / BATCH_SIZE, e);
                failCount += batchCaseIds.size();
            }
        }
    }

    /**
     * 仅对指定用例 ID 列表异步计算 CS 并写入 case_metrics_detail（用于导入后补算，使复用指标可统计）
     */
    @Async("csCalculationExecutor")
    public void batchCalculateMetricsDetailForCaseIdsAsync(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return;
        }
        try {
            processBatch(caseIds, false);
        } catch (Exception e) {
            log.error("导入后 CS 计算失败", e);
        }
    }
    
    /**
     * 批量处理一批用例（批量查询 + 批量写入）
     */
    @Transactional(rollbackFor = Exception.class)
    private BatchProcessResult processBatch(List<String> caseIds, boolean forceRecalculate) {
        BatchProcessResult result = new BatchProcessResult();
        
        if (caseIds == null || caseIds.isEmpty()) {
            return result;
        }
        
        // 1. 批量查询现有数据（判断是否需要跳过）
        Map<String, CaseMetricsDetail> existingMap = new HashMap<>();
        if (!forceRecalculate) {
            List<CaseMetricsDetail> existingList = caseMetricsDetailMapper.selectByCaseIds(caseIds);
            if (existingList != null) {
                for (CaseMetricsDetail detail : existingList) {
                    existingMap.put(detail.getCaseId(), detail);
                }
            }
        }
        
        // 过滤出需要计算的用例ID
        List<String> needCalculateIds = new ArrayList<>();
        for (String caseId : caseIds) {
            if (forceRecalculate || !existingMap.containsKey(caseId)) {
                needCalculateIds.add(caseId);
            } else {
                result.skipCount++;
            }
        }
        
        if (needCalculateIds.isEmpty()) {
            return result;
        }
        
        // 2. 批量查询用例基础数据
        FunctionalCaseExample example = new FunctionalCaseExample();
        example.createCriteria().andIdIn(needCalculateIds);
        List<FunctionalCase> functionalCases = functionalCaseMapper.selectByExample(example);
        Map<String, FunctionalCase> caseMap = new HashMap<>();
        for (FunctionalCase fc : functionalCases) {
            caseMap.put(fc.getId(), fc);
        }
        
        // 3. 批量查询用例详情数据
        FunctionalCaseBlobExample blobExample = new FunctionalCaseBlobExample();
        blobExample.createCriteria().andIdIn(needCalculateIds);
        List<FunctionalCaseBlob> caseBlobs = functionalCaseBlobMapper.selectByExampleWithBLOBs(blobExample);
        Map<String, FunctionalCaseBlob> blobMap = new HashMap<>();
        for (FunctionalCaseBlob blob : caseBlobs) {
            blobMap.put(blob.getId(), blob);
        }
        
        // 4. 批量查询自定义字段数据
        Map<String, List<FunctionalCaseCustomField>> customFieldMap = 
                functionalCaseCustomFieldService.getCustomFieldMapByCaseIds(needCalculateIds);
        
        // 5. 批量计算CS值
        List<CaseMetricsDetail> insertList = new ArrayList<>();
        List<CaseMetricsDetail> updateList = new ArrayList<>();
        
        for (String caseId : needCalculateIds) {
            try {
                FunctionalCase functionalCase = caseMap.get(caseId);
                FunctionalCaseBlob caseBlob = blobMap.get(caseId);
                
                if (functionalCase == null || caseBlob == null) {
                    log.warn("用例数据不完整，跳过: caseId={}", caseId);
                    result.failCount++;
                    continue;
                }
                
                // 使用批量计算方法
                CSCalculationResult csResult = calculateCaseCSInternalBatch(
                        functionalCase, caseBlob, customFieldMap);
                CaseMetricsDetail detail = convertToDetail(csResult);
                
                // 判断是插入还是更新
                CaseMetricsDetail existing = existingMap.get(caseId);
                if (existing == null) {
                    insertList.add(detail);
                } else {
                    // 保留现有数据的关键字段
                    detail.setId(existing.getId());
                    if (existing.getPlatformActualWriteMs() != null && existing.getPlatformActualWriteMs() > 0) {
                        detail.setPlatformActualWriteMs(existing.getPlatformActualWriteMs());
                    }
                    if (existing.getUqsScore() != null) {
                        detail.setUqsScore(existing.getUqsScore());
                    }
                    if (existing.getReuseCount() != null) {
                        detail.setReuseCount(existing.getReuseCount());
                    }
                    if (existing.getModifyCount() != null) {
                        detail.setModifyCount(existing.getModifyCount());
                    }
                    if (existing.getEnvFactor() != null) {
                        detail.setEnvFactor(existing.getEnvFactor());
                        // 如果环境因子不是1.0，需要重新应用环境因子到理论工时
                        if (existing.getEnvFactor().compareTo(BigDecimal.ONE) != 0) {
                            long adjustedWriteMs = (long) (detail.getAlgoExpectedWriteMs() * existing.getEnvFactor().doubleValue());
                            long adjustedExecMs = (long) (detail.getAlgoExpectedExecMs() * existing.getEnvFactor().doubleValue());
                            detail.setAlgoExpectedWriteMs(adjustedWriteMs);
                            detail.setAlgoExpectedExecMs(adjustedExecMs);
                        }
                    }
                    if (existing.getCaseSourceType() != null) {
                        detail.setCaseSourceType(existing.getCaseSourceType());
                    }
                    if (existing.getModificationCostMs() != null) {
                        detail.setModificationCostMs(existing.getModificationCostMs());
                        if ("REUSE".equals(existing.getCaseSourceType()) && existing.getModificationCostMs() > 0) {
                            long savedMs = detail.getAlgoExpectedWriteMs() - existing.getModificationCostMs();
                            detail.setSavedWriteMs(Math.max(0, savedMs));
                        } else {
                            detail.setSavedWriteMs(existing.getSavedWriteMs() != null ? existing.getSavedWriteMs() : 0L);
                        }
                    } else {
                        detail.setSavedWriteMs(0L);
                    }
                    if (existing.getCreateTime() != null) {
                        detail.setCreateTime(existing.getCreateTime());
                    }
                    // 同步 source_case_id、reuse_type 以 functional_case 为准（detail 已从 csResult 带出）
                    if (functionalCase.getSourceCaseId() != null) {
                        detail.setSourceCaseId(functionalCase.getSourceCaseId());
                    }
                    detail.setReuseType(functionalCase.getReuseType());
                    updateList.add(detail);
                }
                
                result.successCount++;
            } catch (Exception e) {
                log.error("计算用例CS值失败，caseId={}", caseId, e);
                result.failCount++;
            }
        }
        
        // 6. 批量写入数据库前，再次检查 insertList 中是否有已存在的记录（防止并发插入）
        if (!insertList.isEmpty()) {
            List<String> insertCaseIds = new ArrayList<>();
            for (CaseMetricsDetail detail : insertList) {
                insertCaseIds.add(detail.getCaseId());
            }
            
            // 再次查询这些 case_id 是否已存在
            List<CaseMetricsDetail> recheckList = caseMetricsDetailMapper.selectByCaseIds(insertCaseIds);
            Map<String, CaseMetricsDetail> recheckMap = new HashMap<>();
            if (recheckList != null) {
                for (CaseMetricsDetail detail : recheckList) {
                    recheckMap.put(detail.getCaseId(), detail);
                }
            }
            
            // 将已存在的记录从 insertList 移到 updateList
            List<CaseMetricsDetail> finalInsertList = new ArrayList<>();
            for (CaseMetricsDetail detail : insertList) {
                CaseMetricsDetail existing = recheckMap.get(detail.getCaseId());
                if (existing != null) {
                    // 已存在，改为更新
                    detail.setId(existing.getId());
                    // 保留现有数据的关键字段
                    if (existing.getPlatformActualWriteMs() != null && existing.getPlatformActualWriteMs() > 0) {
                        detail.setPlatformActualWriteMs(existing.getPlatformActualWriteMs());
                    }
                    if (existing.getUqsScore() != null) {
                        detail.setUqsScore(existing.getUqsScore());
                    }
                    if (existing.getReuseCount() != null) {
                        detail.setReuseCount(existing.getReuseCount());
                    }
                    if (existing.getModifyCount() != null) {
                        detail.setModifyCount(existing.getModifyCount());
                    }
                    if (existing.getEnvFactor() != null) {
                        detail.setEnvFactor(existing.getEnvFactor());
                        if (existing.getEnvFactor().compareTo(BigDecimal.ONE) != 0) {
                            long adjustedWriteMs = (long) (detail.getAlgoExpectedWriteMs() * existing.getEnvFactor().doubleValue());
                            long adjustedExecMs = (long) (detail.getAlgoExpectedExecMs() * existing.getEnvFactor().doubleValue());
                            detail.setAlgoExpectedWriteMs(adjustedWriteMs);
                            detail.setAlgoExpectedExecMs(adjustedExecMs);
                        }
                    }
                    if (existing.getCaseSourceType() != null) {
                        detail.setCaseSourceType(existing.getCaseSourceType());
                    }
                    if (existing.getModificationCostMs() != null) {
                        detail.setModificationCostMs(existing.getModificationCostMs());
                        if ("REUSE".equals(existing.getCaseSourceType()) && existing.getModificationCostMs() > 0) {
                            long savedMs = detail.getAlgoExpectedWriteMs() - existing.getModificationCostMs();
                            detail.setSavedWriteMs(Math.max(0, savedMs));
                        } else {
                            detail.setSavedWriteMs(existing.getSavedWriteMs() != null ? existing.getSavedWriteMs() : 0L);
                        }
                    } else {
                        detail.setSavedWriteMs(0L);
                    }
                    if (existing.getCreateTime() != null) {
                        detail.setCreateTime(existing.getCreateTime());
                    }
                    updateList.add(detail);
                } else {
                    // 不存在，可以插入
                    finalInsertList.add(detail);
                }
            }
            
            // 批量插入
            if (!finalInsertList.isEmpty()) {
                caseMetricsDetailMapper.batchInsert(finalInsertList);
                log.debug("批量插入 {} 条记录", finalInsertList.size());
            }
        }
        
        // 批量更新（循环调用单个更新，因为 MySQL 默认不支持多语句）
        if (!updateList.isEmpty()) {
            int updateCount = 0;
            for (CaseMetricsDetail detail : updateList) {
                try {
                    caseMetricsDetailMapper.updateByPrimaryKey(detail);
                    updateCount++;
                } catch (Exception e) {
                    log.error("更新用例指标详情失败，caseId={}", detail.getCaseId(), e);
                }
            }
            log.debug("批量更新 {} 条记录（成功 {} 条）", updateList.size(), updateCount);
        }
        
        return result;
    }
    
    /**
     * 批量处理结果
     */
    private static class BatchProcessResult {
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
    }
    
    /**
     * 计算并保存单个用例的指标详情（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void calculateAndSaveCaseMetricsDetail(String caseId) {
        CSCalculationResult csResult = calculateCaseCSInternal(caseId);
        CaseMetricsDetail detail = convertToDetail(csResult);
        
        if (caseMetricsDetailMapper.selectByCaseId(caseId) == null) {
            caseMetricsDetailMapper.insert(detail);
        } else {
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
        }
    }
    
    /**
     * 获取需要计算的用例总数
     */
    public int getTotalCaseCount(String projectId) {
        List<String> caseIds = (projectId != null) ?
                extCaseMetricsMapper.getCaseIdsByProjectForBatch(projectId) :
                extCaseMetricsMapper.getAllCaseIds();
        
        return caseIds == null ? 0 : caseIds.size();
    }

    /**
     * 将 CSCalculationResult 转换为 CaseMetricsDetail
     */
    private CaseMetricsDetail convertToDetail(CSCalculationResult csResult) {
        CaseMetricsDetail detail = new CaseMetricsDetail();
        detail.setId(IDGenerator.nextStr());
        detail.setCaseId(csResult.getCaseId());
        detail.setSourceCaseId(csResult.getSourceCaseId());
        detail.setReuseType(csResult.getReuseType());
        detail.setProjectId(csResult.getProjectId());
        detail.setCreateUser(csResult.getCreateUser());
        
        // CS分值和因子
        detail.setCsScore(csResult.getCsScore());
        detail.setCsFactorC1(csResult.getCsFactorC1());
        detail.setCsFactorC2(csResult.getCsFactorC2());
        detail.setCsFactorC3(csResult.getCsFactorC3());
        detail.setCsFactorC4(csResult.getCsFactorC4());
        detail.setCsFactorC5(csResult.getCsFactorC5());
        detail.setCsFactorC6(csResult.getCsFactorC6());
        
        // 复杂度等级（根据CS分值划分）
        detail.setComplexityLevel(calculateComplexityLevel(csResult.getCsScore()));
        
        // 用例类型（默认MANUAL，如果有API关联则为AUTO）
        detail.setCaseType(determineCaseType(csResult.getCaseId()));
        
        // 环境稳定性因子（默认1.0，稳定环境）
        detail.setEnvFactor(BigDecimal.valueOf(1.0));
        
        // 算法理论工时（基于CS分值计算，包含环境因子）
        long baseWriteTimeMs = calculateAlgoExpectedWriteMs(csResult.getCsScore(), detail.getCaseType());
        long baseExecTimeMs = calculateAlgoExpectedExecMs(csResult.getCsScore(), detail.getCaseType());
        detail.setAlgoExpectedWriteMs(baseWriteTimeMs);
        detail.setAlgoExpectedExecMs(baseExecTimeMs);
        
        // 用例来源：有 source_case_id 表示来自两库导出再导入或直接复制两库，记为 REUSE 以参与复用统计
        if (csResult.getSourceCaseId() != null && !csResult.getSourceCaseId().isEmpty()) {
            detail.setCaseSourceType("REUSE");
        } else {
            detail.setCaseSourceType("NEW");
        }
        detail.setModificationCostMs(0L); // 默认修改耗时为0
        detail.setSavedWriteMs(0L); // 默认节约工时为0（新建用例不节约）
        
        long now = System.currentTimeMillis();
        // 创建时间从functional_case表同步，不是计算时的当前时间
        detail.setCreateTime(csResult.getCreateTime());
        detail.setUpdateTime(now);
        detail.setLastCalcTime(now);
        return detail;
    }

    /**
     * 根据CS分值计算复杂度等级
     * L1: CS <= 15 (基础 CRUD级别)
     * L2: 15 < CS <= 30 (中等 流程级别)
     * L3: 30 < CS <= 45 (复杂 领域级别)
     * L4: CS > 45 (高难度 架构/集成级别)
     */
    private String calculateComplexityLevel(BigDecimal csScore) {
        if (csScore == null) {
            return "L1";
        }
        double score = csScore.doubleValue();
        if (score <= 15) {
            return "L1";
        } else if (score <= 30) {
            return "L2";
        } else if (score <= 45) {
            return "L3";
        } else {
            return "L4";
        }
    }

    /**
     * 判断用例类型（MANUAL或AUTO）
     * 简化版：默认为MANUAL，可以根据实际业务逻辑扩展
     */
    private String determineCaseType(String caseId) {
        // 目前都是手工用例，默认返回MANUAL
        return "MANUAL";
    }

    /**
     * 计算算法理论编写工时（毫秒）
     * 基于复杂度等级的标准时间
     * 公式: T = 标准编写时间 × 环境稳定性因子(1.0) × 用例类型因子(1.0)
     */
    private long calculateAlgoExpectedWriteMs(BigDecimal csScore, String caseType) {
        String complexityLevel = calculateComplexityLevel(csScore);
        
        // 根据复杂度等级获取标准编写时间（分钟）
        int standardWriteMinutes;
        switch (complexityLevel) {
            case "L1":
                standardWriteMinutes = 5;   // L1: 5分钟 (基础 CRUD级别)
                break;
            case "L2":
                standardWriteMinutes = 10;  // L2: 10分钟 (中等 流程级别)
                break;
            case "L3":
                standardWriteMinutes = 20;  // L3: 20分钟 (复杂 领域级别)
                break;
            case "L4":
                standardWriteMinutes = 30;  // L4: 30分钟 (高难度 架构/集成级别)
                break;
            default:
                standardWriteMinutes = 5;
        }
        
        // 环境稳定性因子（暂时固定为1.0）
        double envStabilityFactor = 1.0;
        
        // 用例类型因子（手工用例固定为1.0，自动化为1.5）
        double caseTypeFactor = "AUTO".equals(caseType) ? 1.5 : 1.0;
        
        // 计算理论编写时间 = 标准时间 × 环境因子 × 类型因子
        double totalMinutes = standardWriteMinutes * envStabilityFactor * caseTypeFactor;
        
        return (long) (totalMinutes * 60 * 1000); // 转换为毫秒（分钟 × 60秒 × 1000毫秒）
    }

    /**
     * 计算算法理论执行工时（毫秒）
     * 基于复杂度等级的标准时间
     * 公式: E = 标准执行时间 × 环境稳定性因子(1.0) × 用例类型因子(1.0)
     */
    private long calculateAlgoExpectedExecMs(BigDecimal csScore, String caseType) {
        String complexityLevel = calculateComplexityLevel(csScore);
        
        // 根据复杂度等级获取标准执行时间（分钟）
        int standardExecMinutes;
        switch (complexityLevel) {
            case "L1":
                standardExecMinutes = 5;   // L1: 5分钟 (基础 CRUD级别)
                break;
            case "L2":
                standardExecMinutes = 10;  // L2: 10分钟 (中等 流程级别)
                break;
            case "L3":
                standardExecMinutes = 20;  // L3: 20分钟 (复杂 领域级别)
                break;
            case "L4":
                standardExecMinutes = 30;  // L4: 30分钟 (高难度 架构/集成级别)
                break;
            default:
                standardExecMinutes = 5;
        }
        
        // 环境稳定性因子（暂时固定为1.0）
        double envStabilityFactor = 1.0;
        
        // 用例类型因子（手工用例固定为1.0，自动化为1.5）
        double caseTypeFactor = "AUTO".equals(caseType) ? 1.5 : 1.0;
        
        // 计算理论执行时间 = 标准时间 × 环境因子 × 类型因子
        double totalMinutes = standardExecMinutes * envStabilityFactor * caseTypeFactor;
        
        return (long) (totalMinutes * 60 * 1000); // 转换为毫秒（分钟 × 60秒 × 1000毫秒）
    }

    /**
     * 内部计算结果类
     */
    private static class CSCalculationResult {
        private String caseId;
        private String sourceCaseId;
        private String reuseType;
        private String projectId;
        private String createUser;
        private Long createTime;
        private BigDecimal csScore;
        private BigDecimal cognitiveScore;
        private BigDecimal preconditionScore;
        private BigDecimal stepDetailScore;
        private BigDecimal csFactorC1;
        private BigDecimal csFactorC2;
        private BigDecimal csFactorC3;
        private BigDecimal csFactorC4;
        private BigDecimal csFactorC5;
        private BigDecimal csFactorC6;
        private BigDecimal csFactorC7;

        // Getters and Setters
        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }
        
        public String getSourceCaseId() { return sourceCaseId; }
        public void setSourceCaseId(String sourceCaseId) { this.sourceCaseId = sourceCaseId; }
        
        public String getReuseType() { return reuseType; }
        public void setReuseType(String reuseType) { this.reuseType = reuseType; }
        
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getCreateUser() { return createUser; }
        public void setCreateUser(String createUser) { this.createUser = createUser; }
        
        public Long getCreateTime() { return createTime; }
        public void setCreateTime(Long createTime) { this.createTime = createTime; }
        
        public BigDecimal getCsScore() { return csScore; }
        public void setCsScore(BigDecimal csScore) { this.csScore = csScore; }
        
        public BigDecimal getCognitiveScore() { return cognitiveScore; }
        public void setCognitiveScore(BigDecimal cognitiveScore) { this.cognitiveScore = cognitiveScore; }
        
        public BigDecimal getPreconditionScore() { return preconditionScore; }
        public void setPreconditionScore(BigDecimal preconditionScore) { this.preconditionScore = preconditionScore; }
        
        public BigDecimal getStepDetailScore() { return stepDetailScore; }
        public void setStepDetailScore(BigDecimal stepDetailScore) { this.stepDetailScore = stepDetailScore; }
        
        public BigDecimal getCsFactorC1() { return csFactorC1; }
        public void setCsFactorC1(BigDecimal csFactorC1) { this.csFactorC1 = csFactorC1; }
        
        public BigDecimal getCsFactorC2() { return csFactorC2; }
        public void setCsFactorC2(BigDecimal csFactorC2) { this.csFactorC2 = csFactorC2; }
        
        public BigDecimal getCsFactorC3() { return csFactorC3; }
        public void setCsFactorC3(BigDecimal csFactorC3) { this.csFactorC3 = csFactorC3; }
        
        public BigDecimal getCsFactorC4() { return csFactorC4; }
        public void setCsFactorC4(BigDecimal csFactorC4) { this.csFactorC4 = csFactorC4; }
        
        public BigDecimal getCsFactorC5() { return csFactorC5; }
        public void setCsFactorC5(BigDecimal csFactorC5) { this.csFactorC5 = csFactorC5; }
        
        public BigDecimal getCsFactorC6() { return csFactorC6; }
        public void setCsFactorC6(BigDecimal csFactorC6) { this.csFactorC6 = csFactorC6; }
        
        public BigDecimal getCsFactorC7() { return csFactorC7; }
        public void setCsFactorC7(BigDecimal csFactorC7) { this.csFactorC7 = csFactorC7; }
    }
}
