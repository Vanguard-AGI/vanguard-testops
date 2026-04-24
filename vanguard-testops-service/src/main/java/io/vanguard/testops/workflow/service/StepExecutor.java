package io.vanguard.testops.workflow.service;

import java.util.Map;

/**
 * 步骤执行器接口
 */
public interface StepExecutor {
    /**
     * 执行步骤
     * @param stepConfig 步骤配置
     * @param context 执行上下文
     * @return 执行结果
     */
    StepExecutionResult execute(Map<String, Object> stepConfig, ExecutionContext context);
}

