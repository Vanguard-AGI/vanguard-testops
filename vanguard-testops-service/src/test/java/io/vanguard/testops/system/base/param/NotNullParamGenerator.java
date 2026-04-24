package io.vanguard.testops.system.base.param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Jan
 */
public class NotNullParamGenerator extends ParamGenerator {

    /**
     * 返回 null
     */
    @Override
    public Object invalidGenerate(Annotation annotation, Field field) {
        return null;
    }
}
