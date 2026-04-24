package io.vanguard.testops.functional.support.importexport.excel.annotation;

import java.lang.annotation.*;

/**
 * @author Jan
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface NotRequired {
}
