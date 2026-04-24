package io.vanguard.testops.plan.constants;

/**
 * 测试用例执行阻塞原因枚举
 * 用于细分统计阻塞的具体原因，便于分析和优化
 */
public class BlockedReason {
    
    /** 环境因素（如服务器故障、网络波动） */
    public static final String ENVIRONMENT = "ENVIRONMENT";
    
    /** 资源不足阻塞（如人力短缺、硬件/工具未到位） */
    public static final String RESOURCE_SHORTAGE = "RESOURCE_SHORTAGE";
    
    /** 前置依赖阻塞（关联任务/模块未完成交付） */
    public static final String PREREQUISITE_DEPENDENCY = "PREREQUISITE_DEPENDENCY";
    
    /** 需求/方案不明确阻塞（待确认需求细节、方案方向） */
    public static final String REQUIREMENT_UNCLEAR = "REQUIREMENT_UNCLEAR";
    
    /** 技术难点阻塞（遇到未预期的技术问题待解决） */
    public static final String TECHNICAL_DIFFICULTY = "TECHNICAL_DIFFICULTY";
    
    /** 流程/沟通阻塞（审批流程延迟、跨团队沟通未对齐） */
    public static final String PROCESS_COMMUNICATION = "PROCESS_COMMUNICATION";
}

