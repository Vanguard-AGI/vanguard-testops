package io.vanguard.testops.api.support.regex.model;

import io.vanguard.testops.api.support.regex.exception.RegexpIllegalException;
import io.vanguard.testops.api.support.regex.exception.TypeNotMatchException;
import io.vanguard.testops.api.support.regex.exception.UninitializedException;

public interface Node {

    String getExpression();

    String random() throws UninitializedException, RegexpIllegalException;

    boolean test();

    void init() throws RegexpIllegalException, TypeNotMatchException;

    boolean isInitialized();
}
