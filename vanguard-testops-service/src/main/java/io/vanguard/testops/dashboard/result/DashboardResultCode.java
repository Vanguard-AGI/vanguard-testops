package io.vanguard.testops.dashboard.result;

import io.vanguard.testops.sdk.exception.IResultCode;

/**
 * @author Jan
 */
public enum DashboardResultCode implements IResultCode {

    NO_PROJECT_PERMISSION(109001, "no_project_permission");

    private final int code;
    private final String message;

    DashboardResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return getTranslationMessage(this.message);
    }
}
