package io.vanguard.testops.workflow.service;

import org.springframework.stereotype.Component;

/**
 * 步骤执行器工厂
 *
 * <p>Current architecture note:</p>
 * <ul>
 *   <li>the active workflow execution path goes through {@link WorkflowRunService}
 *   and the remote workflow executor service</li>
 *   <li>this factory belongs to the local in-process execution skeleton, which is
 *   not wired into the current runtime path yet</li>
 * </ul>
 *
 * <p>Do not silently return {@code null} here, otherwise callers may mistake the
 * local skeleton for a fully connected execution pipeline.</p>
 */
@Component
public class StepExecutorFactory {

    /**
     * 根据步骤类型获取执行器
     * @param stepType 步骤类型（HTTP/SQL/SCRIPT/CONDITION等）
     * @return 步骤执行器
     */
    public StepExecutor getExecutor(String stepType) {
        if (stepType == null) {
            throw new UnsupportedOperationException("本地工作流执行骨架尚未接入，stepType 不能为空");
        }

        String normalizedStepType = stepType.toUpperCase();
        switch (normalizedStepType) {
            case "HTTP":
            case "API":
            case "SQL":
            case "SCRIPT":
            case "PYTHON":
            case "SHELL":
            case "CONDITION":
            case "IF":
            default:
                throw new UnsupportedOperationException(
                    "当前运行时未启用本地 StepExecutor 路径，未提供步骤类型 [" + normalizedStepType + "] 的本地执行器"
                );
        }
    }
}
