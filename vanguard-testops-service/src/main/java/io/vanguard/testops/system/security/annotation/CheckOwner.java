package io.vanguard.testops.system.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckOwner {
    String resourceId();

    String resourceType();

    String relationType() default "";
}
