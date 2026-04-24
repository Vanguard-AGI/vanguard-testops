package io.vanguard.testops.system.base.param;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Jan
 */
public class NotBlankParamGenerator extends ParamGenerator {

    /**
     * 生成空字符串
     */
    @Override
    public Object invalidGenerate(Annotation annotation, Field field) {
        return StringUtils.EMPTY;
    }
}
