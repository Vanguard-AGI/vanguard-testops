package io.vanguard.testops.functional.constants;

/**
 * 用例效能指标常量
 */
public class CaseMetricsConstants {

    /**
     * 复用类型（从两库 copy 出的用例）
     * 用于 functional_case.reuse_type、统计直接复用/适配复用
     */
    public static class ReuseType {
        /** 直接复用：仅修改了标题 */
        public static final String DIRECT_REUSE = "DIRECT_REUSE";
        /** 适配复用：除标题外还修改了步骤/预期/前置/备注等 */
        public static final String ADAPT_REUSE = "ADAPT_REUSE";
    }

    /**
     * 两库模块名（直接用模块名判断，不做配置）
     * 用于：新增率/变更率分母、复用统计等仅统计两库下用例的口径
     */
    public static class TwoLibraryModuleName {
        /** 用例模板库 */
        public static final String TEMPLATE_LIBRARY = "用例模板库";
        /** 回归用例库 */
        public static final String REGRESSION_LIBRARY = "回归用例库";
    }

    /**
     * 版本用例模块名（新增率分母中排除，不作为底数）
     */
    public static class VersionCaseModuleName {
        /** 迭代版本管理 */
        public static final String ITERATION_VERSION = "迭代版本管理";
    }

    /**
     * 指标级别
     */
    public static class MetricLevel {
        public static final String PROJECT = "PROJECT";  // 项目级
        public static final String PLAN = "PLAN";        // 计划级
        public static final String CASE = "CASE";        // 用例级
        public static final String USER = "USER";        // 用户级
    }

    /**
     * 时间维度
     */
    public static class TimeDimension {
        public static final String DAY = "DAY";          // 日
        public static final String WEEK = "WEEK";        // 周
        public static final String MONTH = "MONTH";      // 月
        public static final String QUARTER = "QUARTER";  // 季
        public static final String YEAR = "YEAR";        // 年
    }

    /**
     * 执行结果
     */
    public static class ExecResult {
        public static final String PASS = "PASS";        // 通过
        public static final String FAIL = "FAIL";        // 失败
        public static final String BLOCKED = "BLOCKED";  // 阻塞
        public static final String SKIP = "SKIP";        // 跳过
    }

    /**
     * 高频用例阈值（执行次数）
     */
    public static final int HIGH_FREQ_THRESHOLD = 3;

    /**
     * CS评分权重
     */
    public static class CSWeights {
        public static final double W1 = 0.5;  // 标题长度权重
        public static final double W2 = 0.5;  // 风险等级权重
        public static final double W3 = 1.5;  // 前置条件数量权重
        public static final double W4 = 4.0;  // 复杂数据准备权重
        public static final double W5 = 1.0;  // 操作步骤数权重
        public static final double W6 = 2.0;  // 验证点数权重
        public static final double W7 = 3.0;  // 逻辑分支数量权重
    }

    /**
     * 标题长度参数
     */
    public static class TitleLength {
        public static final int L_MIN = 10;      // 最佳长度起点
        public static final int L_MAX = 30;      // 最佳长度终点
        public static final double B = 1.0;      // 基础分值
        public static final double P_FACTOR = 0.1; // 惩罚因子
    }
}

