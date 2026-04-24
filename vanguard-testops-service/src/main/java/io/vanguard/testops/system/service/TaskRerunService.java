package io.vanguard.testops.system.service;

import io.vanguard.testops.system.domain.ExecTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public interface TaskRerunService {
    /**
     * 任务重跑
     */
    void rerun(ExecTask execTask, String userId);
}
