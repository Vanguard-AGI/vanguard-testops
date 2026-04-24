package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.dto.request.TrackModificationTimeRequest;
import io.vanguard.testops.functional.dto.request.TrackWriteTimeRequest;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MetricTrackService {

    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;
    @Resource
    private CaseCSCalculationService caseCSCalculationService;

    public void trackWriteTime(TrackWriteTimeRequest request) {
        if (request.getCaseId() == null || request.getDurationMs() == null) {
            return;
        }
        if (request.getCaseId().startsWith("temp-")) {
            LogUtils.info("临时ID {} 跳过CS计算，等待用例保存后使用正式ID再计算", request.getCaseId());
            return;
        }

        CaseMetricsDetail detail = caseMetricsDetailMapper.selectByCaseId(request.getCaseId());
        if (detail != null) {
            long current = detail.getPlatformActualWriteMs() == null ? 0 : detail.getPlatformActualWriteMs();
            detail.setPlatformActualWriteMs(current + request.getDurationMs());
            detail.setUpdateTime(System.currentTimeMillis());
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
            return;
        }

        try {
            LogUtils.info("用例 {} 的 case_metrics_detail 不存在，先计算CS再创建记录", request.getCaseId());
            detail = caseCSCalculationService.getOrCalculateCSDetail(request.getCaseId());
            if (detail == null) {
                LogUtils.warn("用例 {} CS计算失败，无法创建记录", request.getCaseId());
                return;
            }
            long current = detail.getPlatformActualWriteMs() == null ? 0 : detail.getPlatformActualWriteMs();
            detail.setPlatformActualWriteMs(current + request.getDurationMs());
            detail.setUpdateTime(System.currentTimeMillis());
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
            LogUtils.info("为用例 {} 计算CS并保存了编写耗时: {}ms, 理论工时: {}ms",
                    request.getCaseId(), request.getDurationMs(), detail.getAlgoExpectedWriteMs());
        } catch (Exception e) {
            LogUtils.error("计算CS并创建 case_metrics_detail 记录失败，caseId=" + request.getCaseId(), e);
        }
    }

    public void trackModificationTime(TrackModificationTimeRequest request) {
        if (request.getCaseId() == null || request.getModificationCostMs() == null) {
            return;
        }
        try {
            CaseMetricsDetail detail = caseMetricsDetailMapper.selectByCaseId(request.getCaseId());
            if (detail == null) {
                LogUtils.warn("未找到 case_metrics_detail 记录: caseId={}", request.getCaseId());
                return;
            }
            detail.setModificationCostMs(request.getModificationCostMs());
            detail.setCaseSourceType("REUSE");
            if (detail.getAlgoExpectedWriteMs() != null) {
                long savedWriteMs = detail.getAlgoExpectedWriteMs() - request.getModificationCostMs();
                detail.setSavedWriteMs(Math.max(0, savedWriteMs));
            }
            detail.setUpdateTime(System.currentTimeMillis());
            caseMetricsDetailMapper.updateByPrimaryKey(detail);
            LogUtils.info("更新用例修改耗时: caseId={}, modificationCostMs={}ms ({}分钟), savedWriteMs={}ms ({}分钟)",
                    request.getCaseId(),
                    detail.getModificationCostMs(),
                    detail.getModificationCostMs() / 60000,
                    detail.getSavedWriteMs(),
                    detail.getSavedWriteMs() / 60000);
        } catch (Exception e) {
            LogUtils.error("保存修改耗时失败: caseId=" + request.getCaseId(), e);
        }
    }
}
