package io.vanguard.testops.workflow.service;

import java.util.Map;

/**
 * 步骤执行结果
 */
public class StepExecutionResult {
    private boolean success;
    private Map<String, Object> responseData;
    private Map<String, Object> extractVars;
    private String errorMsg;
    private Long durationMs;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getResponseData() {
        return responseData;
    }

    public void setResponseData(Map<String, Object> responseData) {
        this.responseData = responseData;
    }

    public Map<String, Object> getExtractVars() {
        return extractVars;
    }

    public void setExtractVars(Map<String, Object> extractVars) {
        this.extractVars = extractVars;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}

