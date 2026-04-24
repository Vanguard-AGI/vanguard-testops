package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.functional.mapper.FunctionalCaseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class CaseComplexityService {

    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;
    @Resource
    private FunctionalCaseMapper functionalCaseMapper;

    // CS 权重因子
    private static final BigDecimal W1 = new BigDecimal("0.5"); // 认知/风险
    private static final BigDecimal W2 = new BigDecimal("1.5"); // 前置条件
    private static final BigDecimal W3 = new BigDecimal("4.0"); // 数据准备
    private static final BigDecimal W4 = new BigDecimal("1.0"); // 步骤细节
    private static final BigDecimal W5 = new BigDecimal("2.0"); // 验证点
    private static final BigDecimal W6 = new BigDecimal("3.0"); // 逻辑分支

    // 复杂度等级阈值 (示例值，需根据业务调整)
    private static final BigDecimal L1_THRESHOLD = new BigDecimal("5.0");
    private static final BigDecimal L2_THRESHOLD = new BigDecimal("10.0");
    private static final BigDecimal L3_THRESHOLD = new BigDecimal("20.0");

    // 基础工时系数 (毫秒)
    private static final long BASE_WRITE_TIME_MS = 30 * 60 * 1000; // 30分钟
    private static final long BASE_EXEC_TIME_MS = 5 * 60 * 1000;   // 5分钟

    public void calculateAndSave(String caseId) {
        FunctionalCase functionalCase = functionalCaseMapper.selectByPrimaryKey(caseId);
        if (functionalCase == null) {
            return;
        }

        // 1. 提取因子 (这里暂时使用默认值或从用例解析，后续需完善解析逻辑)
        // TODO: 实现从 FunctionalCaseBlob 解析 C1-C6 的逻辑
        BigDecimal c1 = BigDecimal.ZERO; // P0=1, P1=0.5...
        BigDecimal c2 = BigDecimal.ZERO; // 前置条件数
        BigDecimal c3 = BigDecimal.ZERO; // 数据准备难度
        BigDecimal c4 = BigDecimal.ONE; // 步骤数 (占位符)
        BigDecimal c5 = BigDecimal.ZERO; // 验证点数
        BigDecimal c6 = BigDecimal.ZERO; // 逻辑分支

        // 2. 计算 CS Score
        // CS = Σ(Ci * Wi)
        BigDecimal csScore = c1.multiply(W1)
                .add(c2.multiply(W2))
                .add(c3.multiply(W3))
                .add(c4.multiply(W4))
                .add(c5.multiply(W5))
                .add(c6.multiply(W6));

        // 3. 判定等级
        String level = determineLevel(csScore);

        // 4. 计算理论工时 (T = CS * Base)
        long expectedWriteMs = csScore.multiply(BigDecimal.valueOf(BASE_WRITE_TIME_MS)).longValue();
        long expectedExecMs = csScore.multiply(BigDecimal.valueOf(BASE_EXEC_TIME_MS)).longValue();

        // 5. 保存/更新
        CaseMetricsDetail detail = caseMetricsDetailMapper.selectByCaseId(caseId);
        if (detail == null) {
            detail = new CaseMetricsDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setCaseId(caseId);
            detail.setCreateTime(System.currentTimeMillis());
            detail.setPlatformActualWriteMs(0L); // 初始为0
        }
        
        detail.setProjectId(functionalCase.getProjectId());
        detail.setCreateUser(functionalCase.getCreateUser());
        detail.setCsFactorC1(c1);
        detail.setCsFactorC2(c2);
        detail.setCsFactorC3(c3);
        detail.setCsFactorC4(c4);
        detail.setCsFactorC5(c5);
        detail.setCsFactorC6(c6);
        detail.setCsScore(csScore);
        detail.setComplexityLevel(level);
        detail.setAlgoExpectedWriteMs(expectedWriteMs);
        detail.setAlgoExpectedExecMs(expectedExecMs);
        detail.setLastCalcTime(System.currentTimeMillis());
        detail.setUpdateTime(System.currentTimeMillis());

        if (caseMetricsDetailMapper.selectByPrimaryKey(detail.getId()) == null) {
            caseMetricsDetailMapper.insert(detail);
        } else {
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
        }
    }

    private String determineLevel(BigDecimal score) {
        if (score.compareTo(L1_THRESHOLD) <= 0) return "L1";
        if (score.compareTo(L2_THRESHOLD) <= 0) return "L2";
        if (score.compareTo(L3_THRESHOLD) <= 0) return "L3";
        return "L4";
    }

    private int countSteps(String stepsJson) {
        // 简单统计步骤数，实际需解析 JSON
        if (stepsJson == null || stepsJson.isEmpty()) return 0;
        // 假设 steps 是 JSON 数组，简单计算 '{' 数量除以对象特征，或者直接返回默认值
        return 1; 
    }
}
