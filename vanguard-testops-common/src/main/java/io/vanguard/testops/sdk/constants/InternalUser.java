package io.vanguard.testops.sdk.constants;

/**
 * 系统内置用户ID
 * @author Jan
 */
public enum InternalUser {

    ADMIN("admin");

    private String value;

    InternalUser(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
