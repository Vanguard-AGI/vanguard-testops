package io.vanguard.testops.system.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckOrgOwner {

    String resourceId();

    String resourceType();

    String resourceCol() default "organization_id";
}
