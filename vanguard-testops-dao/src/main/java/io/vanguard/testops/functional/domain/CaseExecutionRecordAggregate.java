package io.vanguard.testops.functional.domain;

import java.io.Serializable;

/**
 * case_execution_record 汇总结果
 */
public class CaseExecutionRecordAggregate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long execCount;
    private Long totalDuration;
    private Long passCount;
    private Long firstExecCount;
    private Long firstPassCount;

    public Long getExecCount() {
        return execCount;
    }

    public void setExecCount(Long execCount) {
        this.execCount = execCount;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Long getPassCount() {
        return passCount;
    }

    public void setPassCount(Long passCount) {
        this.passCount = passCount;
    }

    public Long getFirstExecCount() {
        return firstExecCount;
    }

    public void setFirstExecCount(Long firstExecCount) {
        this.firstExecCount = firstExecCount;
    }

    public Long getFirstPassCount() {
        return firstPassCount;
    }

    public void setFirstPassCount(Long firstPassCount) {
        this.firstPassCount = firstPassCount;
    }
}

