package io.vanguard.testops.system.support.metrics;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;

/**
 * 计划模块百分比计算工具类
 */
@UtilityClass
public class RateCalculateUtils {

    public static final int MAX_BOUNDARY = 100;
    public static final int MIN_BOUNDARY = 0;

    public static Double divWithPrecision(Long molecular, Long denominator, Integer precision) {
        DecimalFormat rateFormat = new DecimalFormat("#.##");
        rateFormat.setMinimumFractionDigits(precision);
        rateFormat.setMaximumFractionDigits(precision);
        double rate = (molecular == 0 || denominator == 0) ? 0 :
                Double.parseDouble(rateFormat.format((double) molecular * 100 / (double) denominator));
        if (rate == MAX_BOUNDARY && molecular < denominator) {
            return 99.99;
        } else if (rate == MIN_BOUNDARY && molecular > 0) {
            return 0.01;
        }
        return rate;
    }

    public static Double divWithPrecision(Integer molecular, Integer denominator, Integer precision) {
        return divWithPrecision((long) molecular, (long) denominator, precision);
    }
}
