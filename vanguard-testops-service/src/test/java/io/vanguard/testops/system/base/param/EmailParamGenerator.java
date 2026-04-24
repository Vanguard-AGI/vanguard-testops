package io.vanguard.testops.system.base.param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Jan
 */
public class EmailParamGenerator extends ParamGenerator {

    /**
     * 返回非邮件格式的字符串
     */
    @Override
    public Object invalidGenerate(Annotation annotation, Field field) {
        return "111111111";
    }
}
