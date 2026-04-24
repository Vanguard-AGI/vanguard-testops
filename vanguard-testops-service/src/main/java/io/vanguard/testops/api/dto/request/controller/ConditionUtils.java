package io.vanguard.testops.api.dto.request.controller;

import io.vanguard.testops.sdk.constants.MsAssertionCondition;

public class ConditionUtils {


    public String getConditionValue(String variable, String condition, String value) {
        String wrappedVariable = "\"" + variable + "\"";
        String wrappedValue = "\"" + value + "\"";
        MsAssertionCondition msAssertionCondition = MsAssertionCondition.valueOf(condition);
        switch (msAssertionCondition) {
            case EMPTY:
                return String.format("(%s==\"\" || %s==%s || empty(%s))", wrappedVariable, wrappedVariable,
                        wrappedVariable.replace("$", "\\$"), wrappedVariable);
            case NOT_EMPTY:
                return String.format("(%s!=\"\" && %s!=%s && !empty(%s))", wrappedVariable, wrappedVariable,
                        wrappedVariable.replace("$", "\\$"), wrappedVariable);
            case GT:
                return variable + ">" + value;
            case LT:
                return variable + "<" + value;
            case LT_OR_EQUALS:
                return variable + "<=" + value;
            case GT_OR_EQUALS:
                return variable + ">=" + value;
            case CONTAINS:
                return String.format("%s.contains(%s)", wrappedVariable, wrappedValue);
            case NOT_CONTAINS:
                return String.format("!%s.contains(%s)", wrappedVariable, wrappedValue);
            case EQUALS:
                return isNumeric(value) ? wrappedVariable + "==" + value : wrappedVariable + "==" + wrappedValue;
            case NOT_EQUALS:
                return isNumeric(value) ? wrappedVariable + "!=" + value : wrappedVariable + "!=" + wrappedValue;
            default:
                return wrappedValue;
        }
    }

    public boolean isNumeric(String strNum) {
        try {
            Double.parseDouble(strNum);
            return true;
        } catch (NumberFormatException e2) {
            return false;
        }
    }
}
