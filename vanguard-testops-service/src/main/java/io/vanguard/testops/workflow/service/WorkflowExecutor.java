package io.vanguard.testops.workflow.service;

import io.vanguard.testops.workflow.dto.WorkflowRunStepDTO;

/**
 * 工作流执行器接口
 *
 * <p>This interface models the local in-process workflow execution skeleton.</p>
 *
 * <p>Current production execution does not call this interface directly. Runtime
 * execution flows through {@link WorkflowRunService}, which assembles workflow
 * payloads for the remote executor service and then consumes callback results.</p>
 */
public interface WorkflowExecutor {
    
    /**
     * 异步执行工作流
     * @param runId 运行ID
     * @param workflowId 工作流ID
     * @param environmentId 环境ID
     */
    void executeAsync(String runId, String workflowId, String environmentId);
    
    /**
     * 执行单个步骤
     * @param stepId 步骤ID
     * @param context 执行上下文（包含变量、环境信息等）
     * @return 步骤执行结果
     */
    WorkflowRunStepDTO executeStep(String stepId, ExecutionContext context);
    
    /**
     * 取消执行
     * @param runId 运行ID
     */
    void cancel(String runId);
}
