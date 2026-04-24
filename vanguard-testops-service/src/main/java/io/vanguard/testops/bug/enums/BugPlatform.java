package io.vanguard.testops.bug.enums;

import lombok.Getter;

/**
 * @author Jan
 */

@Getter
public enum BugPlatform {

    /**
     * 本地
     */
    LOCAL("Local"),
    JIRA("JIRA"),
    ZENTAO("禅道"),
    TAPD("TAPD"),
    FEISHU("FEISHU");


    private final String name;

    BugPlatform(String name) {
        this.name = name;
    }
}
