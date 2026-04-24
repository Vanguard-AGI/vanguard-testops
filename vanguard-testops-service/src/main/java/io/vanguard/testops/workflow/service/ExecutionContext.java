package io.vanguard.testops.workflow.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文
 */
public class ExecutionContext {
    private Map<String, Object> variables = new HashMap<>();
    private String environmentId;
    private String runId;
    
    // Getters and Setters
    public Map<String, Object> getVariables() {
        return variables;
    }
    
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
    
    public String getEnvironmentId() {
        return environmentId;
    }
    
    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
}

