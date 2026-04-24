package io.vanguard.testops.system.job;


import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.vanguard.testops.sdk.constants.ParamConstants;
import io.vanguard.testops.sdk.domain.OperationLogBlobExample;
import io.vanguard.testops.sdk.mapper.OperationLogBlobMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.SubListUtils;
import io.vanguard.testops.system.domain.SystemParameter;
import io.vanguard.testops.system.mapper.BaseOperationHistoryMapper;
import io.vanguard.testops.system.mapper.BaseOperationLogMapper;
import io.vanguard.testops.system.mapper.SystemParameterMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CleanHistoryJob {

    @Resource
    private SystemParameterMapper systemParameterMapper;
    @Resource
    private BaseOperationHistoryMapper baseOperationHistoryMapper;
    @Resource
    private BaseOperationLogMapper baseOperationLogMapper;
    @Resource
    private OperationLogBlobMapper operationLogBlobMapper;

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_LIMIT_MAX = 100001;

    /**
     * 清理变更历史 每天凌晨两点执行
     */
    @QuartzScheduled(cron = "0 0 2 * * ?")
    public void cleanupLog() {
        LogUtils.info("clean up history start.");
        SystemParameter parameter = systemParameterMapper.selectByPrimaryKey(ParamConstants.CleanConfig.OPERATION_HISTORY.getValue());
        Optional.ofNullable(parameter).ifPresentOrElse(
                p -> {
                    int limit = Integer.parseInt(p.getParamValue());
                    if (limit == DEFAULT_LIMIT_MAX) {
                        return;
                    }
                    doCleanupHistory(limit);
                },
                () -> {
                    doCleanupHistory(DEFAULT_LIMIT);
                }
        );
        LogUtils.info("clean up log end.");
    }

    private void doCleanupHistory(int limit) {
        try {
            //变更历史处理
            List<String> sourceIds = baseOperationHistoryMapper.selectSourceIds(limit);
            SubListUtils.dealForSubList(sourceIds, 100, subList -> {
                cleanupHistory(subList, limit);
            });
        } catch (Exception e) {
            LogUtils.error(e);
        }

    }

    public void cleanupHistory(List<String> batch, int limit) {
        batch.forEach(sourceId -> {
            List<Long> ids = baseOperationHistoryMapper.selectIdsBySourceId(sourceId, limit);
            if (CollectionUtils.isNotEmpty(ids)) {
                baseOperationHistoryMapper.deleteBySourceId(sourceId, ids);
                List<Long> logIds = baseOperationLogMapper.selectIdByHistoryIds(ids);
                ids.removeAll(logIds);
                if (CollectionUtils.isNotEmpty(ids)) {
                    OperationLogBlobExample blobExample = new OperationLogBlobExample();
                    blobExample.createCriteria().andIdIn(ids);
                    operationLogBlobMapper.deleteByExample(blobExample);
                }
            }
        });
    }
}