package io.vanguard.testops.bug.constants;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 飞书 Meego 缺陷字段选项与系统落库值的写死映射（统一在此维护，方便管理）。
 * - 缺陷类型/发现人：落库飞书选项 ID，前端 value=id、label=中文。
 * - 状态/发现阶段/优先级：落库展示值（中文或 P0-P4），前端 value=展示值。
 */
public final class FeishuOptionMapping {

    private FeishuOptionMapping() {
    }

    // ==================== 缺陷类型 template ====================
    /** 飞书缺陷类型：中文 label → 飞书选项 ID（落库用） */
    private static final Map<String, String> DEFECT_TYPE_LABEL_TO_ID = Map.ofEntries(
            Map.entry("功能性缺陷", "1063335"),
            Map.entry("性能缺陷", "1063338"),
            Map.entry("界面缺陷", "1063339"),
            Map.entry("兼容性缺陷", "1063340"),
            Map.entry("安全性缺陷", "1063341"),
            Map.entry("可靠性缺陷", "1063342"),
            Map.entry("易用性缺陷", "1063343")
    );
    /** 飞书缺陷类型所有选项 ID（用于判断 raw 是否已是 id） */
    private static final Set<String> DEFECT_TYPE_IDS = Set.of(
            "1063335", "1063338", "1063339", "1063340", "1063341", "1063342", "1063343"
    );

    /**
     * 将飞书返回的缺陷类型值（可能是 id 或中文）统一为飞书选项 ID 落库。
     */
    public static String toDefectTypeId(String raw) {
        if (StringUtils.isBlank(raw)) return "";
        String t = raw.trim();
        if (DEFECT_TYPE_IDS.contains(t)) return t;
        return DEFECT_TYPE_LABEL_TO_ID.getOrDefault(t, t);
    }

    // ==================== 发现人 field_f12022 ====================
    /** 飞书发现人：中文 label → 飞书选项 ID（落库用） */
    private static final Map<String, String> DISCOVERER_LABEL_TO_ID = Map.ofEntries(
            Map.entry("客户", "ni44ivb3f"),
            Map.entry("运营", "fz1ucyq94"),
            Map.entry("财务", "8l_px3ic5"),
            Map.entry("核对平台", "fck_4ikcj"),
            Map.entry("产品", "1djafdjzn"),
            Map.entry("开发", "j__nd637a"),
            Map.entry("测试", "h2c8psxg7"),
            Map.entry("自动化测试", "k_x1_h_wz"),
            Map.entry("巡检(拨测)平台", "nprb0vbwo"),
            Map.entry("巡检（拨测）平台", "nprb0vbwo"),
            Map.entry("日志告警", "9izhmhih76")
    );
    private static final Set<String> DISCOVERER_IDS = Set.of(
            "ni44ivb3f", "fz1ucyq94", "8l_px3ic5", "fck_4ikcj", "1djafdjzn",
            "j__nd637a", "h2c8psxg7", "k_x1_h_wz", "nprb0vbwo", "9izhmhih76"
    );

    /**
     * 将飞书返回的发现人值（可能是 id 或中文）统一为飞书选项 ID 落库。
     */
    public static String toDiscovererId(String raw) {
        if (StringUtils.isBlank(raw)) return "";
        String t = raw.trim();
        if (DISCOVERER_IDS.contains(t)) return t;
        return DISCOVERER_LABEL_TO_ID.getOrDefault(t, t);
    }

    // ==================== 状态 work_item_status ====================
    /** 飞书状态选项ID（高级配置中的选项ID）→ 系统状态展示值（中文，落库用） */
    private static final Map<String, String> STATUS_STATE_KEY_TO_DISPLAY = Map.ofEntries(
            Map.entry("Not started", "待确认"),
            Map.entry("In Progress", "处理中"),
            Map.entry("fYFKOOeAM", "已解决"),
            Map.entry("BBteJzss3", "已关闭"),
            Map.entry("FNB4WAesv", "再次打开"),
            Map.entry("xO-EjNWsO", "暂不修复"),
            Map.entry("_zqtitjjqb", "拒绝"),
            Map.entry("dge61mypx", "已验证")
    );
    /** 系统状态中文集合（用于判断 raw 是否已是展示值） */
    private static final Set<String> STATUS_DISPLAY_VALUES = Set.of(
            "待确认", "处理中", "已解决", "暂不修复", "拒绝", "已关闭", "已完成", "再次打开", "已验证"
    );

    /**
     * 飞书状态（state_key 或中文）→ 系统状态展示值（中文）落库。
     * 与飞书/系统侧展示一致；缺省返回「待确认」。
     */
    public static String toStatusDisplay(String raw) {
        if (StringUtils.isBlank(raw)) return "待确认";
        String t = raw.trim();
        if (STATUS_DISPLAY_VALUES.contains(t)) return t;
        return STATUS_STATE_KEY_TO_DISPLAY.getOrDefault(t, t);
    }

    /** 系统状态展示值（中文）→ 飞书 state_key（同步到飞书时用） */
    private static final Map<String, String> STATUS_DISPLAY_TO_STATE_KEY = Map.ofEntries(
            Map.entry("待确认", "Not started"),
            Map.entry("处理中", "In Progress"),
            Map.entry("已关闭", "BBteJzss3"),
            Map.entry("再次打开", "FNB4WAesv"),
            Map.entry("暂不修复", "xO-EjNWsO"),
            Map.entry("拒绝", "_zqtitjjqb"),
            Map.entry("已验证", "dge61mypx"),
            Map.entry("已解决", "BBteJzss3"),
            Map.entry("已完成", "dge61mypx")
    );

    public static String toStatusStateKey(String display) {
        if (StringUtils.isBlank(display)) return "Not started";
        String t = display.trim();
        return STATUS_DISPLAY_TO_STATE_KEY.getOrDefault(t, "Not started");
    }

    // ==================== 发现阶段 field_1cbc4e ====================
    /** 发现阶段合法展示值（与前端 BUG_DISCOVERY_PHASE_OPTIONS 一致），落库即展示值 */
    private static final Set<String> DISCOVERY_PHASE_DISPLAY_VALUES = Set.of(
            "联调阶段", "测试阶段", "冒烟测试", "第一轮测试", "第二轮测试",
            "回归阶段", "UI/UE/PM验收", "线上阶段"
    );
    /** 飞书发现阶段可能返回的 key/id → 中文（若飞书后续给选项 id 可在此补充） */
    private static final Map<String, String> DISCOVERY_PHASE_KEY_TO_DISPLAY = Map.of();

    /**
     * 飞书发现阶段（id 或中文）→ 系统展示值（中文）落库。
     * 已是合法中文则原样返回，否则查映射表，都没有则原样返回。
     */
    public static String toDiscoveryPhaseDisplay(String raw) {
        if (StringUtils.isBlank(raw)) return "";
        String t = raw.trim();
        if (DISCOVERY_PHASE_DISPLAY_VALUES.contains(t)) return t;
        return DISCOVERY_PHASE_KEY_TO_DISPLAY.getOrDefault(t, t);
    }

    // ==================== 缺陷发现难易度 field_6b822e ====================
    /** 飞书缺陷发现难易度选项 ID → 系统展示值（落库用） */
    private static final Map<String, String> DISCOVERY_DIFFICULTY_OPTION_ID_TO_DISPLAY = Map.of(
            "3ns4vp022", "容易",
            "qqjd77cjh", "一般",
            "b7j2ahwbe", "困难"
    );
    private static final Set<String> DISCOVERY_DIFFICULTY_DISPLAY_VALUES = Set.of("容易", "一般", "困难");

    /**
     * 飞书缺陷发现难易度（选项 id 或已是中文）→ 系统展示值落库。
     */
    public static String toDiscoveryDifficultyDisplay(String raw) {
        if (StringUtils.isBlank(raw)) return "";
        String t = raw.trim();
        if (DISCOVERY_DIFFICULTY_DISPLAY_VALUES.contains(t)) return t;
        return DISCOVERY_DIFFICULTY_OPTION_ID_TO_DISPLAY.getOrDefault(t, t);
    }

    /** 系统展示值（容易/一般/困难）→ 飞书选项 ID（同步到飞书时用） */
    private static final Map<String, String> DISCOVERY_DIFFICULTY_DISPLAY_TO_OPTION_ID = Map.of(
            "容易", "3ns4vp022",
            "一般", "qqjd77cjh",
            "困难", "b7j2ahwbe"
    );

    public static String toDiscoveryDifficultyOptionId(String display) {
        if (StringUtils.isBlank(display)) return "";
        return DISCOVERY_DIFFICULTY_DISPLAY_TO_OPTION_ID.getOrDefault(display.trim(), "");
    }

    // ==================== 优先级 priority ====================
    /** 飞书优先级选项 id → 系统展示值 P0-P4（落库用） */
    private static final Map<String, String> PRIORITY_OPTION_ID_TO_DISPLAY = Map.ofEntries(
            Map.entry("option_1", "P0"),
            Map.entry("option_2", "P1"),
            Map.entry("option_3", "P2"),
            Map.entry("option_4", "P3"),
            Map.entry("option_5", "P4"),
            Map.entry("jih_ogigt", "P3"),
            Map.entry("r1ozs01jx", "P4")
    );
    private static final Set<String> PRIORITY_DISPLAY_VALUES = Set.of("P0", "P1", "P2", "P3", "P4");

    /**
     * 飞书优先级（选项 id 或已是 P0-P4）→ 系统展示值 P0-P4 落库。
     */
    public static String toPriorityDisplay(String raw) {
        if (StringUtils.isBlank(raw)) return "";
        String t = raw.trim();
        if (PRIORITY_DISPLAY_VALUES.contains(t)) return t;
        return PRIORITY_OPTION_ID_TO_DISPLAY.getOrDefault(t, t);
    }

    /** 系统展示值 P0-P4 → 飞书优先级选项 id（同步到飞书时用） */
    private static final Map<String, String> PRIORITY_DISPLAY_TO_OPTION_ID = Map.ofEntries(
            Map.entry("P0", "option_1"),
            Map.entry("P1", "option_2"),
            Map.entry("P2", "option_3"),
            Map.entry("P3", "option_4"),
            Map.entry("P4", "option_5")
    );

    public static String toPriorityOptionId(String display) {
        if (StringUtils.isBlank(display)) return "";
        String t = display.trim();
        return PRIORITY_DISPLAY_TO_OPTION_ID.getOrDefault(t, "");
    }

    // ==================== 业务线（来自 docs/映射表.json，严格按文件顺序，不做任何排序） ====================

    /** 业务线 id → "level|path"（按映射表顺序，供前端树展示和 id↔name 映射） */
    private static final java.util.LinkedHashMap<String, String[]> BUSINESS_LINE_MAP = new java.util.LinkedHashMap<>();
    static {
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669979", new String[]{"商家&运营", "0", "商家&运营"});
        BUSINESS_LINE_MAP.put("663c37fff12350b7f87d951f", new String[]{"客户关系", "1", "商家&运营 > 客户关系"});
        BUSINESS_LINE_MAP.put("663dfaad691259e96574c79a", new String[]{"CRM", "2", "商家&运营 > 客户关系 > CRM"});
        BUSINESS_LINE_MAP.put("667929fa910c73c27d58e2e2", new String[]{"客户管理", "3", "商家&运营 > 客户关系 > CRM > 客户管理"});
        BUSINESS_LINE_MAP.put("66792a0b9cd4daf3f2236d76", new String[]{"售前报价", "3", "商家&运营 > 客户关系 > CRM > 售前报价"});
        BUSINESS_LINE_MAP.put("66792a5ce6f243593cbea4f9", new String[]{"合同管理", "3", "商家&运营 > 客户关系 > CRM > 合同管理"});
        BUSINESS_LINE_MAP.put("66792a759cd4daf3f2236d77", new String[]{"渠道管理", "3", "商家&运营 > 客户关系 > CRM > 渠道管理"});
        BUSINESS_LINE_MAP.put("66792a88910c73c27d58e2e3", new String[]{"销售管理", "3", "商家&运营 > 客户关系 > CRM > 销售管理"});
        BUSINESS_LINE_MAP.put("674434ffcc2d9ec9e2f5ae35", new String[]{"数据看板", "3", "商家&运营 > 客户关系 > CRM > 数据看板"});
        BUSINESS_LINE_MAP.put("6744352d880daeaee5defe2c", new String[]{"工作台", "3", "商家&运营 > 客户关系 > CRM > 工作台"});
        BUSINESS_LINE_MAP.put("66792b353432ca9db2c862ef", new String[]{"供应商", "2", "商家&运营 > 客户关系 > 供应商"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669984", new String[]{"供应商管理", "3", "商家&运营 > 客户关系 > 供应商 > 供应商管理"});
        BUSINESS_LINE_MAP.put("66792b96d473d0a1a6abd44b", new String[]{"运营日志", "3", "商家&运营 > 客户关系 > 供应商 > 运营日志"});
        BUSINESS_LINE_MAP.put("66792bc29cfb8c47fdbd6bf7", new String[]{"标签管理", "3", "商家&运营 > 客户关系 > 供应商 > 标签管理"});
        BUSINESS_LINE_MAP.put("66792bd3910c73c27d58e2e4", new String[]{"数据表现", "3", "商家&运营 > 客户关系 > 供应商 > 数据表现"});
        BUSINESS_LINE_MAP.put("66792bdf8e134edc80856f75", new String[]{"绩效管理", "3", "商家&运营 > 客户关系 > 供应商 > 绩效管理"});
        BUSINESS_LINE_MAP.put("66792c046482393162aa1dc1", new String[]{"合同台账", "3", "商家&运营 > 客户关系 > 供应商 > 合同台账"});
        BUSINESS_LINE_MAP.put("667961ecad0313b6cb0ca63c", new String[]{"CSM", "2", "商家&运营 > 客户关系 > CSM"});
        BUSINESS_LINE_MAP.put("6744354ccc2d9ec9e2f5ae36", new String[]{"咨询类工单", "3", "商家&运营 > 客户关系 > CSM > 咨询类工单"});
        BUSINESS_LINE_MAP.put("67443556b195ed4cc9c0a3c1", new String[]{"智能客服", "3", "商家&运营 > 客户关系 > CSM > 智能客服"});
        BUSINESS_LINE_MAP.put("67455e0c33ee6c0bac34a7f9", new String[]{"平台规则", "2", "商家&运营 > 客户关系 > 平台规则"});
        BUSINESS_LINE_MAP.put("667ab75c8e134edc80856f96", new String[]{"其它", "2", "商家&运营 > 客户关系 > 其它"});
        BUSINESS_LINE_MAP.put("6899ad623c248f877174effc", new String[]{"客户管理", "2", "商家&运营 > 客户关系 > 客户管理"});
        BUSINESS_LINE_MAP.put("6899ad71a52e141882e9a010", new String[]{"客户信息", "3", "商家&运营 > 客户关系 > 客户管理 > 客户信息"});
        BUSINESS_LINE_MAP.put("6899ade8c775b3f15e219c6d", new String[]{"客户标签", "3", "商家&运营 > 客户关系 > 客户管理 > 客户标签"});
        BUSINESS_LINE_MAP.put("6899adef37fb9d5ad2887d4f", new String[]{"客户来源", "3", "商家&运营 > 客户关系 > 客户管理 > 客户来源"});
        BUSINESS_LINE_MAP.put("6899adf5b61e6887a8c84c38", new String[]{"客户阶段", "3", "商家&运营 > 客户关系 > 客户管理 > 客户阶段"});
        BUSINESS_LINE_MAP.put("6899adfd37fb9d5ad2887d50", new String[]{"客户公海", "3", "商家&运营 > 客户关系 > 客户管理 > 客户公海"});
        BUSINESS_LINE_MAP.put("6899ae05307e734a309f5d46", new String[]{"数据同步", "3", "商家&运营 > 客户关系 > 客户管理 > 数据同步"});
        BUSINESS_LINE_MAP.put("6899ae0d77588eff8c6d03b3", new String[]{"联系人管理", "2", "商家&运营 > 客户关系 > 联系人管理"});
        BUSINESS_LINE_MAP.put("6899ae1ba581729c4dfc1db5", new String[]{"联系人信息", "3", "商家&运营 > 客户关系 > 联系人管理 > 联系人信息"});
        BUSINESS_LINE_MAP.put("6899ae2278f648f44212be6f", new String[]{"联系人关系网", "3", "商家&运营 > 客户关系 > 联系人管理 > 联系人关系网"});
        BUSINESS_LINE_MAP.put("6899ae3d70b80bfb78161580", new String[]{"线索管理", "2", "商家&运营 > 客户关系 > 线索管理"});
        BUSINESS_LINE_MAP.put("6899aea5c775b3f15e219c6f", new String[]{"线索获取", "3", "商家&运营 > 客户关系 > 线索管理 > 线索获取"});
        BUSINESS_LINE_MAP.put("6899aeab78f648f44212be71", new String[]{"线索清洗", "3", "商家&运营 > 客户关系 > 线索管理 > 线索清洗"});
        BUSINESS_LINE_MAP.put("6899aeb2a52e141882e9a012", new String[]{"线索分配", "3", "商家&运营 > 客户关系 > 线索管理 > 线索分配"});
        BUSINESS_LINE_MAP.put("6899aeb8dda07a916539775d", new String[]{"线索转化", "3", "商家&运营 > 客户关系 > 线索管理 > 线索转化"});
        BUSINESS_LINE_MAP.put("6899aec099a76d146be290e6", new String[]{"线索回收", "3", "商家&运营 > 客户关系 > 线索管理 > 线索回收"});
        BUSINESS_LINE_MAP.put("6899ae462913c47377ae3938", new String[]{"商机管理", "2", "商家&运营 > 客户关系 > 商机管理"});
        BUSINESS_LINE_MAP.put("6899aecb74b1e900f14e171b", new String[]{"商机培育", "3", "商家&运营 > 客户关系 > 商机管理 > 商机培育"});
        BUSINESS_LINE_MAP.put("6899aed199a76d146be290e7", new String[]{"商机转化", "3", "商家&运营 > 客户关系 > 商机管理 > 商机转化"});
        BUSINESS_LINE_MAP.put("6899ae54a52e141882e9a011", new String[]{"合同管理", "2", "商家&运营 > 客户关系 > 合同管理"});
        BUSINESS_LINE_MAP.put("6899aedcb3237ed6967a35cb", new String[]{"框架合同", "3", "商家&运营 > 客户关系 > 合同管理 > 框架合同"});
        BUSINESS_LINE_MAP.put("6899aeef3c248f877174effd", new String[]{"SEVC 采购合同", "4", "商家&运营 > 客户关系 > 合同管理 > 框架合同 > SEVC 采购合同"});
        BUSINESS_LINE_MAP.put("6899aee27ae23b08cdd10063", new String[]{"补充协议", "3", "商家&运营 > 客户关系 > 合同管理 > 补充协议"});
        BUSINESS_LINE_MAP.put("6899af002c56b147cd5a328b", new String[]{"结算条款", "3", "商家&运营 > 客户关系 > 合同管理 > 结算条款"});
        BUSINESS_LINE_MAP.put("6899af0cc5df6c51b93beaa3", new String[]{"SEVC 账期条款", "4", "商家&运营 > 客户关系 > 合同管理 > 结算条款 > SEVC 账期条款"});
        BUSINESS_LINE_MAP.put("6899af15c775b3f15e219c70", new String[]{"线上签约", "3", "商家&运营 > 客户关系 > 合同管理 > 线上签约"});
        BUSINESS_LINE_MAP.put("6899af1bd43d351987748a38", new String[]{"流程配置", "3", "商家&运营 > 客户关系 > 合同管理 > 流程配置"});
        BUSINESS_LINE_MAP.put("6899ae5f9d6d793664aa79a7", new String[]{"售前报价", "2", "商家&运营 > 客户关系 > 售前报价"});
        BUSINESS_LINE_MAP.put("6899af2bd43d351987748a39", new String[]{"报价上传", "3", "商家&运营 > 客户关系 > 售前报价 > 报价上传"});
        BUSINESS_LINE_MAP.put("6899af30b61e6887a8c84c39", new String[]{"审核处理", "3", "商家&运营 > 客户关系 > 售前报价 > 审核处理"});
        BUSINESS_LINE_MAP.put("6899af3611de3902f68ae2bc", new String[]{"智能报价", "3", "商家&运营 > 客户关系 > 售前报价 > 智能报价"});
        BUSINESS_LINE_MAP.put("6899af3c1a87287cf9ec25b4", new String[]{"流程配置", "3", "商家&运营 > 客户关系 > 售前报价 > 流程配置"});
        BUSINESS_LINE_MAP.put("6899ae6c78f648f44212be70", new String[]{"业务管理", "2", "商家&运营 > 客户关系 > 业务管理"});
        BUSINESS_LINE_MAP.put("6899af61dda07a916539775e", new String[]{"客户拜访", "3", "商家&运营 > 客户关系 > 业务管理 > 客户拜访"});
        BUSINESS_LINE_MAP.put("6899af67ebea94d8ba2d86bf", new String[]{"工作周报", "3", "商家&运营 > 客户关系 > 业务管理 > 工作周报"});
        BUSINESS_LINE_MAP.put("6899af6c9db5f6e43e7534ac", new String[]{"客户清退", "3", "商家&运营 > 客户关系 > 业务管理 > 客户清退"});
        BUSINESS_LINE_MAP.put("6899ae73bcfb5c817cb1549f", new String[]{"数据看板", "2", "商家&运营 > 客户关系 > 数据看板"});
        BUSINESS_LINE_MAP.put("6899af7d70b80bfb78161581", new String[]{"指标仪表盘", "3", "商家&运营 > 客户关系 > 数据看板 > 指标仪表盘"});
        BUSINESS_LINE_MAP.put("6899af8abcc248f2b64360a6", new String[]{"明细报告", "3", "商家&运营 > 客户关系 > 数据看板 > 明细报告"});
        BUSINESS_LINE_MAP.put("6899ae7d85a954b004948b09", new String[]{"客户服务", "2", "商家&运营 > 客户关系 > 客户服务"});
        BUSINESS_LINE_MAP.put("6899af95ebea94d8ba2d86c1", new String[]{"智能客服", "3", "商家&运营 > 客户关系 > 客户服务 > 智能客服"});
        BUSINESS_LINE_MAP.put("6899af9b9db5f6e43e7534ad", new String[]{"客服工单", "3", "商家&运营 > 客户关系 > 客户服务 > 客服工单"});
        BUSINESS_LINE_MAP.put("6899afa685a954b004948b0a", new String[]{"内部协同", "3", "商家&运营 > 客户关系 > 客户服务 > 内部协同"});
        BUSINESS_LINE_MAP.put("6899ae87d43d351987748a37", new String[]{"供应商管理", "2", "商家&运营 > 客户关系 > 供应商管理"});
        BUSINESS_LINE_MAP.put("6899afb2b81b0366a066d968", new String[]{"供应商信息", "3", "商家&运营 > 客户关系 > 供应商管理 > 供应商信息"});
        BUSINESS_LINE_MAP.put("6899afc611de3902f68ae2bd", new String[]{"供应商标签", "3", "商家&运营 > 客户关系 > 供应商管理 > 供应商标签"});
        BUSINESS_LINE_MAP.put("6899afd215b57bdd44374f92", new String[]{"生命周期", "3", "商家&运营 > 客户关系 > 供应商管理 > 生命周期"});
        BUSINESS_LINE_MAP.put("6899afe0b61e6887a8c84c3a", new String[]{"数据表现", "3", "商家&运营 > 客户关系 > 供应商管理 > 数据表现"});
        BUSINESS_LINE_MAP.put("6899affc99a76d146be290e8", new String[]{"供应商状态", "3", "商家&运营 > 客户关系 > 供应商管理 > 供应商状态"});
        BUSINESS_LINE_MAP.put("6899b0071a87287cf9ec25b5", new String[]{"供应商基建", "3", "商家&运营 > 客户关系 > 供应商管理 > 供应商基建"});
        BUSINESS_LINE_MAP.put("6899b00ee76c8669b7a76ee2", new String[]{"评论", "4", "商家&运营 > 客户关系 > 供应商管理 > 供应商基建 > 评论"});
        BUSINESS_LINE_MAP.put("6899b0142c56b147cd5a328c", new String[]{"操作日志", "4", "商家&运营 > 客户关系 > 供应商管理 > 供应商基建 > 操作日志"});
        BUSINESS_LINE_MAP.put("6899b01fb81b0366a066d969", new String[]{"合同与条款", "3", "商家&运营 > 客户关系 > 供应商管理 > 合同与条款"});
        BUSINESS_LINE_MAP.put("689ab507b81b0366a066d96b", new String[]{"基础建设", "2", "商家&运营 > 客户关系 > 基础建设"});
        BUSINESS_LINE_MAP.put("689ab7ad342b395259d68ca8", new String[]{"交互视觉", "3", "商家&运营 > 客户关系 > 基础建设 > 交互视觉"});
        BUSINESS_LINE_MAP.put("689ab7b3e3f7103b36833a17", new String[]{"权限管理", "3", "商家&运营 > 客户关系 > 基础建设 > 权限管理"});
        BUSINESS_LINE_MAP.put("689ab7b93c248f877174f002", new String[]{"消息中心", "3", "商家&运营 > 客户关系 > 基础建设 > 消息中心"});
        BUSINESS_LINE_MAP.put("689ab82c2913c47377ae3953", new String[]{"导出中心", "3", "商家&运营 > 客户关系 > 基础建设 > 导出中心"});
        BUSINESS_LINE_MAP.put("663c382ff12350b7f87d9520", new String[]{"营销中心", "1", "商家&运营 > 营销中心"});
        BUSINESS_LINE_MAP.put("66792e3d3432ca9db2c862f0", new String[]{"营销活动", "2", "商家&运营 > 营销中心 > 营销活动"});
        BUSINESS_LINE_MAP.put("66792e63910c73c27d58e2e6", new String[]{"AMZ Coupon", "3", "商家&运营 > 营销中心 > 营销活动 > AMZ Coupon"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66997a", new String[]{"AMZ Promotion", "3", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion"});
        BUSINESS_LINE_MAP.put("66792e8d9e3b88dff995778b", new String[]{"Promo Code", "4", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion > Promo Code"});
        BUSINESS_LINE_MAP.put("66792eb2e6f243593cbea4fa", new String[]{"Price Discount", "4", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion > Price Discount"});
        BUSINESS_LINE_MAP.put("682c322d3b07d1fbbb90e219", new String[]{"AMZ Promotion Central", "3", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion Central"});
        BUSINESS_LINE_MAP.put("66792e739e3b88dff995778a", new String[]{"Best Deal", "4", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion Central > Best Deal"});
        BUSINESS_LINE_MAP.put("66792eccb93c1fa1fcf9da82", new String[]{"Lightning Deal", "4", "商家&运营 > 营销中心 > 营销活动 > AMZ Promotion Central > Lightning Deal"});
        BUSINESS_LINE_MAP.put("6899c7d770b80bfb78161582", new String[]{"AMZ 订购省", "3", "商家&运营 > 营销中心 > 营销活动 > AMZ 订购省"});
        BUSINESS_LINE_MAP.put("674436a833ee6c0bac34a7e1", new String[]{"通用能力", "3", "商家&运营 > 营销中心 > 营销活动 > 通用能力"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998a", new String[]{"广告系统", "2", "商家&运营 > 营销中心 > 广告系统"});
        BUSINESS_LINE_MAP.put("67443b94dc657aed546e0195", new String[]{"AMZ 广告活动", "3", "商家&运营 > 营销中心 > 广告系统 > AMZ 广告活动"});
        BUSINESS_LINE_MAP.put("66792f22f2a33fb1197a5d43", new String[]{"SP", "4", "商家&运营 > 营销中心 > 广告系统 > AMZ 广告活动 > SP"});
        BUSINESS_LINE_MAP.put("66792f2ce6f243593cbea4fb", new String[]{"SD", "4", "商家&运营 > 营销中心 > 广告系统 > AMZ 广告活动 > SD"});
        BUSINESS_LINE_MAP.put("66792f36d473d0a1a6abd44c", new String[]{"SB", "4", "商家&运营 > 营销中心 > 广告系统 > AMZ 广告活动 > SB"});
        BUSINESS_LINE_MAP.put("66792f478e134edc80856f76", new String[]{"广告组合", "4", "商家&运营 > 营销中心 > 广告系统 > AMZ 广告活动 > 广告组合"});
        BUSINESS_LINE_MAP.put("66792f60f2a33fb1197a5d44", new String[]{"广告授权", "3", "商家&运营 > 营销中心 > 广告系统 > 广告授权"});
        BUSINESS_LINE_MAP.put("6899c8094492568a028bc60d", new String[]{"AMZ 广告授权", "4", "商家&运营 > 营销中心 > 广告系统 > 广告授权 > AMZ 广告授权"});
        BUSINESS_LINE_MAP.put("6899c81028252e39e2ec5b8b", new String[]{"供应商广告授权", "4", "商家&运营 > 营销中心 > 广告系统 > 广告授权 > 供应商广告授权"});
        BUSINESS_LINE_MAP.put("66792f538771acd9f73f7373", new String[]{"广告报告", "3", "商家&运营 > 营销中心 > 广告系统 > 广告报告"});
        BUSINESS_LINE_MAP.put("6899c822812487ae9d4639d6", new String[]{"AMZ 广告报告", "4", "商家&运营 > 营销中心 > 广告系统 > 广告报告 > AMZ 广告报告"});
        BUSINESS_LINE_MAP.put("682c31fe7016e8396bb31a76", new String[]{"智能广告", "3", "商家&运营 > 营销中心 > 广告系统 > 智能广告"});
        BUSINESS_LINE_MAP.put("6899c831812487ae9d4639d7", new String[]{"AMZ 智能广告", "4", "商家&运营 > 营销中心 > 广告系统 > 智能广告 > AMZ 智能广告"});
        BUSINESS_LINE_MAP.put("67443bbaa8bd1a896f63edac", new String[]{"广告基建", "3", "商家&运营 > 营销中心 > 广告系统 > 广告基建"});
        BUSINESS_LINE_MAP.put("6899c84385a954b004948b0e", new String[]{"渠道管理", "2", "商家&运营 > 营销中心 > 渠道管理"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669989", new String[]{"SAS", "3", "商家&运营 > 营销中心 > 渠道管理 > SAS"});
        BUSINESS_LINE_MAP.put("66792f0e9cfb8c47fdbd6bf8", new String[]{"SAS Promotion", "4", "商家&运营 > 营销中心 > 渠道管理 > SAS > SAS Promotion"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998b", new String[]{"OD邮箱", "3", "商家&运营 > 营销中心 > 渠道管理 > OD邮箱"});
        BUSINESS_LINE_MAP.put("6899c88111de3902f68ae2be", new String[]{"任务中心", "3", "商家&运营 > 营销中心 > 渠道管理 > 任务中心"});
        BUSINESS_LINE_MAP.put("6899c886342b395259d68c9f", new String[]{"通知中心", "3", "商家&运营 > 营销中心 > 渠道管理 > 通知中心"});
        BUSINESS_LINE_MAP.put("67b2f936f3e1aca8931ef5f6", new String[]{"合规风控", "2", "商家&运营 > 营销中心 > 合规风控"});
        BUSINESS_LINE_MAP.put("6899c8a97ae23b08cdd10066", new String[]{"AMZ 合规认证", "3", "商家&运营 > 营销中心 > 合规风控 > AMZ 合规认证"});
        BUSINESS_LINE_MAP.put("6899c8b6dda07a9165397760", new String[]{"Product Certificates", "4", "商家&运营 > 营销中心 > 合规风控 > AMZ 合规认证 > Product Certificates"});
        BUSINESS_LINE_MAP.put("6899c8bc28252e39e2ec5b8c", new String[]{"Product Compliance", "4", "商家&运营 > 营销中心 > 合规风控 > AMZ 合规认证 > Product Compliance"});
        BUSINESS_LINE_MAP.put("6899c8aec775b3f15e219c73", new String[]{"AMZ POA", "3", "商家&运营 > 营销中心 > 合规风控 > AMZ POA"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669982", new String[]{"数据报告", "2", "商家&运营 > 营销中心 > 数据报告"});
        BUSINESS_LINE_MAP.put("6899c8d3dda07a9165397761", new String[]{"业务报告", "3", "商家&运营 > 营销中心 > 数据报告 > 业务报告"});
        BUSINESS_LINE_MAP.put("6899c8e8d43d351987748a3b", new String[]{"AMZ 销售报告", "4", "商家&运营 > 营销中心 > 数据报告 > 业务报告 > AMZ 销售报告"});
        BUSINESS_LINE_MAP.put("6899c8efdda07a9165397762", new String[]{"AMZ 退货报告", "4", "商家&运营 > 营销中心 > 数据报告 > 业务报告 > AMZ 退货报告"});
        BUSINESS_LINE_MAP.put("6899c8d84bca2e2a6fd7af41", new String[]{"功能分析", "3", "商家&运营 > 营销中心 > 数据报告 > 功能分析"});
        BUSINESS_LINE_MAP.put("6899c8e0812487ae9d4639d8", new String[]{"控制塔", "4", "商家&运营 > 营销中心 > 数据报告 > 功能分析 > 控制塔"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998f", new String[]{"帮助体系", "2", "商家&运营 > 营销中心 > 帮助体系"});
        BUSINESS_LINE_MAP.put("674436c7fb9a275486910421", new String[]{"初始化引导", "3", "商家&运营 > 营销中心 > 帮助体系 > 初始化引导"});
        BUSINESS_LINE_MAP.put("67443ae3dc657aed546e0194", new String[]{"步骤式引导", "3", "商家&运营 > 营销中心 > 帮助体系 > 步骤式引导"});
        BUSINESS_LINE_MAP.put("67443af3a8f9d493b6b077fb", new String[]{"体验反馈", "3", "商家&运营 > 营销中心 > 帮助体系 > 体验反馈"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66997f", new String[]{"知识库", "2", "商家&运营 > 营销中心 > 知识库"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669981", new String[]{"行动计划", "2", "商家&运营 > 营销中心 > 行动计划"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669980", new String[]{"操作日志", "2", "商家&运营 > 营销中心 > 操作日志"});
        BUSINESS_LINE_MAP.put("66792c466482393162aa1dc2", new String[]{"业务管理", "2", "商家&运营 > 营销中心 > 业务管理"});
        BUSINESS_LINE_MAP.put("66792c57910c73c27d58e2e5", new String[]{"工作台", "3", "商家&运营 > 营销中心 > 业务管理 > 工作台"});
        BUSINESS_LINE_MAP.put("66793c76910c73c27d58e2ec", new String[]{"智能化运营", "2", "商家&运营 > 营销中心 > 智能化运营"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669986", new String[]{"营收管理", "3", "商家&运营 > 营销中心 > 智能化运营 > 营收管理"});
        BUSINESS_LINE_MAP.put("66793cb09cfb8c47fdbd6bfa", new String[]{"发货建议", "3", "商家&运营 > 营销中心 > 智能化运营 > 发货建议"});
        BUSINESS_LINE_MAP.put("66793cbd9cd4daf3f2236d78", new String[]{"Review监测", "3", "商家&运营 > 营销中心 > 智能化运营 > Review监测"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66997d", new String[]{"客服系统（废弃）", "2", "商家&运营 > 营销中心 > 客服系统（废弃）"});
        BUSINESS_LINE_MAP.put("667ab765e6f243593cbea512", new String[]{"其它", "2", "商家&运营 > 营销中心 > 其它"});
        BUSINESS_LINE_MAP.put("689ab8387ae23b08cdd10069", new String[]{"基础建设", "2", "商家&运营 > 营销中心 > 基础建设"});
        BUSINESS_LINE_MAP.put("689ab87ec775b3f15e219c78", new String[]{"交互视觉", "3", "商家&运营 > 营销中心 > 基础建设 > 交互视觉"});
        BUSINESS_LINE_MAP.put("689ab883a581729c4dfc1dbb", new String[]{"权限管理", "3", "商家&运营 > 营销中心 > 基础建设 > 权限管理"});
        BUSINESS_LINE_MAP.put("689ab888427943bddf615941", new String[]{"消息中心", "3", "商家&运营 > 营销中心 > 基础建设 > 消息中心"});
        BUSINESS_LINE_MAP.put("689ab88dac1776e0e7ae428c", new String[]{"导出中心", "3", "商家&运营 > 营销中心 > 基础建设 > 导出中心"});
        BUSINESS_LINE_MAP.put("6679229db93c1fa1fcf9da80", new String[]{"商品中心", "1", "商家&运营 > 商品中心"});
        BUSINESS_LINE_MAP.put("667aae39d430da01882188ca", new String[]{"品牌管理", "2", "商家&运营 > 商品中心 > 品牌管理"});
        BUSINESS_LINE_MAP.put("6899c4f04492568a028bc60c", new String[]{"品牌入驻", "3", "商家&运营 > 商品中心 > 品牌管理 > 品牌入驻"});
        BUSINESS_LINE_MAP.put("6899c4f5078060dd3e7e3c74", new String[]{"品牌授权", "3", "商家&运营 > 商品中心 > 品牌管理 > 品牌授权"});
        BUSINESS_LINE_MAP.put("6899c5012c56b147cd5a328d", new String[]{"商标审查", "3", "商家&运营 > 商品中心 > 品牌管理 > 商标审查"});
        BUSINESS_LINE_MAP.put("6899c506e3f7103b36833a09", new String[]{"流程配置", "3", "商家&运营 > 商品中心 > 品牌管理 > 流程配置"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998d", new String[]{"产品中心", "2", "商家&运营 > 商品中心 > 产品中心"});
        BUSINESS_LINE_MAP.put("667aae2a9e3b88dff99577c5", new String[]{"产品管理", "3", "商家&运营 > 商品中心 > 产品中心 > 产品管理"});
        BUSINESS_LINE_MAP.put("6899c519420ed3721ee1cc5f", new String[]{"产品导入", "3", "商家&运营 > 商品中心 > 产品中心 > 产品导入"});
        BUSINESS_LINE_MAP.put("6899c51fa52e141882e9a013", new String[]{"产品信息", "3", "商家&运营 > 商品中心 > 产品中心 > 产品信息"});
        BUSINESS_LINE_MAP.put("667aae459cd4daf3f2236d8a", new String[]{"类目管理", "3", "商家&运营 > 商品中心 > 产品中心 > 类目管理"});
        BUSINESS_LINE_MAP.put("6899c52715b57bdd44374f93", new String[]{"数据同步", "3", "商家&运营 > 商品中心 > 产品中心 > 数据同步"});
        BUSINESS_LINE_MAP.put("6899c545bcfb5c817cb154a0", new String[]{"价格中心", "2", "商家&运营 > 商品中心 > 价格中心"});
        BUSINESS_LINE_MAP.put("6899c553c775b3f15e219c71", new String[]{"产品报价", "3", "商家&运营 > 商品中心 > 价格中心 > 产品报价"});
        BUSINESS_LINE_MAP.put("6899c56ea581729c4dfc1db6", new String[]{"报价申请", "4", "商家&运营 > 商品中心 > 价格中心 > 产品报价 > 报价申请"});
        BUSINESS_LINE_MAP.put("6899c5755f61969739eb571e", new String[]{"审核处理", "4", "商家&运营 > 商品中心 > 价格中心 > 产品报价 > 审核处理"});
        BUSINESS_LINE_MAP.put("6899c57adda07a916539775f", new String[]{"智能报价", "4", "商家&运营 > 商品中心 > 价格中心 > 产品报价 > 智能报价"});
        BUSINESS_LINE_MAP.put("6899c580dbe197213ff3bb5e", new String[]{"流程配置", "4", "商家&运营 > 商品中心 > 价格中心 > 产品报价 > 流程配置"});
        BUSINESS_LINE_MAP.put("6899c559dbe197213ff3bb5d", new String[]{"产品调价", "3", "商家&运营 > 商品中心 > 价格中心 > 产品调价"});
        BUSINESS_LINE_MAP.put("6899c58abcfb5c817cb154a1", new String[]{"调价发起", "4", "商家&运营 > 商品中心 > 价格中心 > 产品调价 > 调价发起"});
        BUSINESS_LINE_MAP.put("6899c591342b395259d68c9e", new String[]{"调价校验", "4", "商家&运营 > 商品中心 > 价格中心 > 产品调价 > 调价校验"});
        BUSINESS_LINE_MAP.put("6899c5977ae23b08cdd10065", new String[]{"流程配置", "4", "商家&运营 > 商品中心 > 价格中心 > 产品调价 > 流程配置"});
        BUSINESS_LINE_MAP.put("6899c55f7ae23b08cdd10064", new String[]{"报价协议", "3", "商家&运营 > 商品中心 > 价格中心 > 报价协议"});
        BUSINESS_LINE_MAP.put("6899c5a1c775b3f15e219c72", new String[]{"VC 报价协议", "4", "商家&运营 > 商品中心 > 价格中心 > 报价协议 > VC 报价协议"});
        BUSINESS_LINE_MAP.put("6899c5664bca2e2a6fd7af40", new String[]{"报价工具", "3", "商家&运营 > 商品中心 > 价格中心 > 报价工具"});
        BUSINESS_LINE_MAP.put("6899c5aa9db5f6e43e7534af", new String[]{"VC 报价测算", "4", "商家&运营 > 商品中心 > 价格中心 > 报价工具 > VC 报价测算"});
        BUSINESS_LINE_MAP.put("6899c5c9e76c8669b7a76ee3", new String[]{"VC 利润估算", "4", "商家&运营 > 商品中心 > 价格中心 > 报价工具 > VC 利润估算"});
        BUSINESS_LINE_MAP.put("66794057f2a33fb1197a5d49", new String[]{"渠道产品中心", "2", "商家&运营 > 商品中心 > 渠道产品中心"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669985", new String[]{"Amazon Listing", "3", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing"});
        BUSINESS_LINE_MAP.put("66795ed968badb862e5c4e2f", new String[]{"类目管理", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > 类目管理"});
        BUSINESS_LINE_MAP.put("66795eef706223ea53dc588b", new String[]{"品牌管理", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > 品牌管理"});
        BUSINESS_LINE_MAP.put("66795efc427dc2a4fae3eae9", new String[]{"Listing上传", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing上传"});
        BUSINESS_LINE_MAP.put("6899c5fbb81b0366a066d96a", new String[]{"DF 克隆", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > DF 克隆"});
        BUSINESS_LINE_MAP.put("6899c60674b1e900f14e171c", new String[]{"Listing 同步", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing 同步"});
        BUSINESS_LINE_MAP.put("6899c60b85a954b004948b0d", new String[]{"Listing 管理", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing 管理"});
        BUSINESS_LINE_MAP.put("6899c611ebea94d8ba2d86c2", new String[]{"Listing 接单状态", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing 接单状态"});
        BUSINESS_LINE_MAP.put("6899c615ebea94d8ba2d86c3", new String[]{"Listing 操作日志", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing 操作日志"});
        BUSINESS_LINE_MAP.put("6899c61bc5df6c51b93beab8", new String[]{"Listing 解析", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Listing 解析"});
        BUSINESS_LINE_MAP.put("6899c6203e1004d083caae9c", new String[]{"Main Listing", "4", "商家&运营 > 商品中心 > 渠道产品中心 > Amazon Listing > Main Listing"});
        BUSINESS_LINE_MAP.put("66795f1188dc2cc20467cd60", new String[]{"Shipify产品中心", "3", "商家&运营 > 商品中心 > 渠道产品中心 > Shipify产品中心"});
        BUSINESS_LINE_MAP.put("66795f1eee7d5435b635855d", new String[]{"Temu产品中心", "3", "商家&运营 > 商品中心 > 渠道产品中心 > Temu产品中心"});
        BUSINESS_LINE_MAP.put("66795f2a06e6ffbbb24621f5", new String[]{"Tiktok产品中心", "3", "商家&运营 > 商品中心 > 渠道产品中心 > Tiktok产品中心"});
        BUSINESS_LINE_MAP.put("6899c648ac1776e0e7ae4288", new String[]{"产品工具", "2", "商家&运营 > 商品中心 > 产品工具"});
        BUSINESS_LINE_MAP.put("6899c65f2c56b147cd5a328e", new String[]{"智能运营", "3", "商家&运营 > 商品中心 > 产品工具 > 智能运营"});
        BUSINESS_LINE_MAP.put("6899c66a48a7a0c2c4f91792", new String[]{"产品分析", "4", "商家&运营 > 商品中心 > 产品工具 > 智能运营 > 产品分析"});
        BUSINESS_LINE_MAP.put("6899c66f1a87287cf9ec25ca", new String[]{"AMZ NPPM Data", "4", "商家&运营 > 商品中心 > 产品工具 > 智能运营 > AMZ NPPM Data"});
        BUSINESS_LINE_MAP.put("6899c664307e734a309f5d48", new String[]{"监控预警", "3", "商家&运营 > 商品中心 > 产品工具 > 监控预警"});
        BUSINESS_LINE_MAP.put("6899c6758ff08e07702281e2", new String[]{"AMZ Listing 监控", "4", "商家&运营 > 商品中心 > 产品工具 > 监控预警 > AMZ Listing 监控"});
        BUSINESS_LINE_MAP.put("6899c682b61e6887a8c84c43", new String[]{"价格监控", "4", "商家&运营 > 商品中心 > 产品工具 > 监控预警 > 价格监控"});
        BUSINESS_LINE_MAP.put("6679608b4a4df566f0cbd513", new String[]{"业务工单", "2", "商家&运营 > 商品中心 > 业务工单"});
        BUSINESS_LINE_MAP.put("6679609e06e6ffbbb24621f6", new String[]{"AMZ Listing 工单", "3", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单"});
        BUSINESS_LINE_MAP.put("6679610906e6ffbbb24621f7", new String[]{"图片", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > 图片"});
        BUSINESS_LINE_MAP.put("66796118706223ea53dc588c", new String[]{"视频", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > 视频"});
        BUSINESS_LINE_MAP.put("66796122706223ea53dc588d", new String[]{"A+", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > A+"});
        BUSINESS_LINE_MAP.put("667961336849d28808cc80cf", new String[]{"Variation", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > Variation"});
        BUSINESS_LINE_MAP.put("66796149ee7d5435b6358560", new String[]{"Newer Model", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > Newer Model"});
        BUSINESS_LINE_MAP.put("667961564d74a1a5df8b4aa1", new String[]{"目录变更", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > 目录变更"});
        BUSINESS_LINE_MAP.put("667961619c18c775caf18006", new String[]{"包装认证", "4", "商家&运营 > 商品中心 > 业务工单 > AMZ Listing 工单 > 包装认证"});
        BUSINESS_LINE_MAP.put("6899c6e5a52e141882e9a014", new String[]{"渠道管理", "2", "商家&运营 > 商品中心 > 渠道管理"});
        BUSINESS_LINE_MAP.put("6899c6f6dbe197213ff3bb5f", new String[]{"VC 账号管理", "3", "商家&运营 > 商品中心 > 渠道管理 > VC 账号管理"});
        BUSINESS_LINE_MAP.put("6899c705307e734a309f5d49", new String[]{"账号费率", "4", "商家&运营 > 商品中心 > 渠道管理 > VC 账号管理 > 账号费率"});
        BUSINESS_LINE_MAP.put("6899c6fe8ff08e07702281e3", new String[]{"SAS", "3", "商家&运营 > 商品中心 > 渠道管理 > SAS"});
        BUSINESS_LINE_MAP.put("6899c71299a76d146be29101", new String[]{"SAS Catalog", "4", "商家&运营 > 商品中心 > 渠道管理 > SAS > SAS Catalog"});
        BUSINESS_LINE_MAP.put("6899c719427943bddf61593e", new String[]{"SAS Others", "4", "商家&运营 > 商品中心 > 渠道管理 > SAS > SAS Others"});
        BUSINESS_LINE_MAP.put("66795f46f1cf370901c899d4", new String[]{"SAS系统", "2", "商家&运营 > 商品中心 > SAS系统"});
        BUSINESS_LINE_MAP.put("66795f539cbc7ad912aafbff", new String[]{"Price", "3", "商家&运营 > 商品中心 > SAS系统 > Price"});
        BUSINESS_LINE_MAP.put("66795f65427dc2a4fae3eaea", new String[]{"Catalog & Brand", "3", "商家&运营 > 商品中心 > SAS系统 > Catalog & Brand"});
        BUSINESS_LINE_MAP.put("66795f859cbc7ad912aafc00", new String[]{"报价中心", "2", "商家&运营 > 商品中心 > 报价中心"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66997c", new String[]{"亚马逊报价", "3", "商家&运营 > 商品中心 > 报价中心 > 亚马逊报价"});
        BUSINESS_LINE_MAP.put("66795e6868badb862e5c4e2e", new String[]{"产品调价-对客", "4", "商家&运营 > 商品中心 > 报价中心 > 亚马逊报价 > 产品调价-对客"});
        BUSINESS_LINE_MAP.put("66795e784d74a1a5df8b4aa0", new String[]{"产品报价-对客", "4", "商家&运营 > 商品中心 > 报价中心 > 亚马逊报价 > 产品报价-对客"});
        BUSINESS_LINE_MAP.put("66795e8f06e6ffbbb24621f4", new String[]{"产品调价-对渠道", "4", "商家&运营 > 商品中心 > 报价中心 > 亚马逊报价 > 产品调价-对渠道"});
        BUSINESS_LINE_MAP.put("674412375d344f85eb312dcd", new String[]{"产品报价-对渠道", "4", "商家&运营 > 商品中心 > 报价中心 > 亚马逊报价 > 产品报价-对渠道"});
        BUSINESS_LINE_MAP.put("682e9b74f93389062d62bb7d", new String[]{"业务管理", "2", "商家&运营 > 商品中心 > 业务管理"});
        BUSINESS_LINE_MAP.put("682e9b7c9c907e3795600753", new String[]{"工作台", "3", "商家&运营 > 商品中心 > 业务管理 > 工作台"});
        BUSINESS_LINE_MAP.put("66795f9bee7d5435b635855e", new String[]{"数据分析", "2", "商家&运营 > 商品中心 > 数据分析"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998c", new String[]{"运营管理池", "3", "商家&运营 > 商品中心 > 数据分析 > 运营管理池"});
        BUSINESS_LINE_MAP.put("66795fc0ee7d5435b635855f", new String[]{"产品分析", "3", "商家&运营 > 商品中心 > 数据分析 > 产品分析"});
        BUSINESS_LINE_MAP.put("66795fd688dc2cc20467cd61", new String[]{"NPPM管理池", "3", "商家&运营 > 商品中心 > 数据分析 > NPPM管理池"});
        BUSINESS_LINE_MAP.put("66795fe86849d28808cc80ce", new String[]{"Listing质量看板", "3", "商家&运营 > 商品中心 > 数据分析 > Listing质量看板"});
        BUSINESS_LINE_MAP.put("682efb00d66c9415da6fa5ca", new String[]{"Reports", "3", "商家&运营 > 商品中心 > 数据分析 > Reports"});
        BUSINESS_LINE_MAP.put("667961749c18c775caf18007", new String[]{"渠道账号管理", "2", "商家&运营 > 商品中心 > 渠道账号管理"});
        BUSINESS_LINE_MAP.put("6679618294a7c3e312ca508c", new String[]{"亚马逊账号", "3", "商家&运营 > 商品中心 > 渠道账号管理 > 亚马逊账号"});
        BUSINESS_LINE_MAP.put("667961909c18c775caf18008", new String[]{"VC账号健康", "4", "商家&运营 > 商品中心 > 渠道账号管理 > 亚马逊账号 > VC账号健康"});
        BUSINESS_LINE_MAP.put("6679619d9c18c775caf18009", new String[]{"费率管理", "4", "商家&运营 > 商品中心 > 渠道账号管理 > 亚马逊账号 > 费率管理"});
        BUSINESS_LINE_MAP.put("667ab770d430da01882188cd", new String[]{"其它", "2", "商家&运营 > 商品中心 > 其它"});
        BUSINESS_LINE_MAP.put("6899c8fc2913c47377ae3939", new String[]{"数据报告", "2", "商家&运营 > 商品中心 > 数据报告"});
        BUSINESS_LINE_MAP.put("6899c91748a7a0c2c4f91793", new String[]{"业务报告", "3", "商家&运营 > 商品中心 > 数据报告 > 业务报告"});
        BUSINESS_LINE_MAP.put("6899c92f4bca2e2a6fd7af43", new String[]{"AMZ 类目报告", "4", "商家&运营 > 商品中心 > 数据报告 > 业务报告 > AMZ 类目报告"});
        BUSINESS_LINE_MAP.put("6899c934c5df6c51b93beab9", new String[]{"AMZ  客户之声", "4", "商家&运营 > 商品中心 > 数据报告 > 业务报告 > AMZ  客户之声"});
        BUSINESS_LINE_MAP.put("6899c93bb0a1615eb26739d5", new String[]{"AMZ Concession hub", "4", "商家&运营 > 商品中心 > 数据报告 > 业务报告 > AMZ Concession hub"});
        BUSINESS_LINE_MAP.put("6899c91c4bca2e2a6fd7af42", new String[]{"功能分析", "3", "商家&运营 > 商品中心 > 数据报告 > 功能分析"});
        BUSINESS_LINE_MAP.put("6899c92577588eff8c6d03b4", new String[]{"控制塔", "4", "商家&运营 > 商品中心 > 数据报告 > 功能分析 > 控制塔"});
        BUSINESS_LINE_MAP.put("689ab8cbbcc248f2b64360a9", new String[]{"基础建设", "2", "商家&运营 > 商品中心 > 基础建设"});
        BUSINESS_LINE_MAP.put("689ab9324bca2e2a6fd7af46", new String[]{"交互视觉", "3", "商家&运营 > 商品中心 > 基础建设 > 交互视觉"});
        BUSINESS_LINE_MAP.put("689ab93a342b395259d68ca9", new String[]{"权限管理", "3", "商家&运营 > 商品中心 > 基础建设 > 权限管理"});
        BUSINESS_LINE_MAP.put("689ab93e2c56b147cd5a32a9", new String[]{"消息中心", "3", "商家&运营 > 商品中心 > 基础建设 > 消息中心"});
        BUSINESS_LINE_MAP.put("689ab9457ae23b08cdd1006a", new String[]{"导出中心", "3", "商家&运营 > 商品中心 > 基础建设 > 导出中心"});
        BUSINESS_LINE_MAP.put("668e35ca915d8e9adb90bf0b", new String[]{"其他", "1", "商家&运营 > 其他"});
        BUSINESS_LINE_MAP.put("668e3620b67baae56c47fdf0", new String[]{"多渠道站点", "2", "商家&运营 > 其他 > 多渠道站点"});
        BUSINESS_LINE_MAP.put("6899c972c5df6c51b93beaba", new String[]{"公共基建", "1", "商家&运营 > 公共基建"});
        BUSINESS_LINE_MAP.put("6899c9a1e3f7103b36833a0a", new String[]{"工作台", "2", "商家&运营 > 公共基建 > 工作台"});
        BUSINESS_LINE_MAP.put("6899c9b4bcfb5c817cb154a2", new String[]{"CRM 工作台", "3", "商家&运营 > 公共基建 > 工作台 > CRM 工作台"});
        BUSINESS_LINE_MAP.put("6899c9bd4492568a028bc60e", new String[]{"SEVC 工作台", "3", "商家&运营 > 公共基建 > 工作台 > SEVC 工作台"});
        BUSINESS_LINE_MAP.put("6899c9c79d6d793664aa79c0", new String[]{"Gmesh 工作台", "3", "商家&运营 > 公共基建 > 工作台 > Gmesh 工作台"});
        BUSINESS_LINE_MAP.put("6899c9ccbcfb5c817cb154a3", new String[]{"CSM 工作台", "3", "商家&运营 > 公共基建 > 工作台 > CSM 工作台"});
        BUSINESS_LINE_MAP.put("6899c9d14d4b9dbae7d9b1e6", new String[]{"Corin 工作台", "3", "商家&运营 > 公共基建 > 工作台 > Corin 工作台"});
        BUSINESS_LINE_MAP.put("6899c9ab37fb9d5ad2887d51", new String[]{"SEVC 平台设施", "2", "商家&运营 > 公共基建 > SEVC 平台设施"});
        BUSINESS_LINE_MAP.put("6899c9dcbcfb5c817cb154a4", new String[]{"SEVC 账户注册", "3", "商家&运营 > 公共基建 > SEVC 平台设施 > SEVC 账户注册"});
        BUSINESS_LINE_MAP.put("6899c9e946b6e865241237bc", new String[]{"SEVC 站点权限", "3", "商家&运营 > 公共基建 > SEVC 平台设施 > SEVC 站点权限"});
        BUSINESS_LINE_MAP.put("6899c9ef3c248f877174effe", new String[]{"SEVC 消息接收人", "3", "商家&运营 > 公共基建 > SEVC 平台设施 > SEVC 消息接收人"});
        BUSINESS_LINE_MAP.put("6899c9f8307e734a309f5d4a", new String[]{"SEVC 平台规则", "3", "商家&运营 > 公共基建 > SEVC 平台设施 > SEVC 平台规则"});
        BUSINESS_LINE_MAP.put("6899c9fd4492568a028bc60f", new String[]{"SEVC 初始化引导", "3", "商家&运营 > 公共基建 > SEVC 平台设施 > SEVC 初始化引导"});
        BUSINESS_LINE_MAP.put("6679233b6482393162aa1dc0", new String[]{"效率协同", "0", "效率协同"});
        BUSINESS_LINE_MAP.put("6679620b706223ea53dc588e", new String[]{"业务中台", "1", "效率协同 > 业务中台"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66998e", new String[]{"权限中心", "2", "效率协同 > 业务中台 > 权限中心"});
        BUSINESS_LINE_MAP.put("667aaf12d473d0a1a6abd469", new String[]{"账号管理", "3", "效率协同 > 业务中台 > 权限中心 > 账号管理"});
        BUSINESS_LINE_MAP.put("667aaf1ff2a33fb1197a5d70", new String[]{"租户管理", "3", "效率协同 > 业务中台 > 权限中心 > 租户管理"});
        BUSINESS_LINE_MAP.put("667aaf2c6482393162aa1de6", new String[]{"组织架构", "3", "效率协同 > 业务中台 > 权限中心 > 组织架构"});
        BUSINESS_LINE_MAP.put("690af395d654a498cb04daaf", new String[]{"成员管理", "3", "效率协同 > 业务中台 > 权限中心 > 成员管理"});
        BUSINESS_LINE_MAP.put("667aaf46910c73c27d58e310", new String[]{"应用管理", "3", "效率协同 > 业务中台 > 权限中心 > 应用管理"});
        BUSINESS_LINE_MAP.put("690af3c2ddd12c2394256ca6", new String[]{"资源管理", "3", "效率协同 > 业务中台 > 权限中心 > 资源管理"});
        BUSINESS_LINE_MAP.put("667aaf509cfb8c47fdbd6c0e", new String[]{"权限管理", "3", "效率协同 > 业务中台 > 权限中心 > 权限管理"});
        BUSINESS_LINE_MAP.put("690af344a0e8eb6bfbc67c50", new String[]{"角色管理", "4", "效率协同 > 业务中台 > 权限中心 > 权限管理 > 角色管理"});
        BUSINESS_LINE_MAP.put("690af34bf42007a912488831", new String[]{"数据权限", "4", "效率协同 > 业务中台 > 权限中心 > 权限管理 > 数据权限"});
        BUSINESS_LINE_MAP.put("69450805fb5500dc42a303a5", new String[]{"模拟登录", "4", "效率协同 > 业务中台 > 权限中心 > 权限管理 > 模拟登录"});
        BUSINESS_LINE_MAP.put("694b5f8d65f9e909ae095e46", new String[]{"临时权限申请", "4", "效率协同 > 业务中台 > 权限中心 > 权限管理 > 临时权限申请"});
        BUSINESS_LINE_MAP.put("667aaf5ff2a33fb1197a5d71", new String[]{"渠道站点管理", "3", "效率协同 > 业务中台 > 权限中心 > 渠道站点管理"});
        BUSINESS_LINE_MAP.put("667aaf6ab93c1fa1fcf9daa7", new String[]{"日志管理", "3", "效率协同 > 业务中台 > 权限中心 > 日志管理"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66997e", new String[]{"消息中心", "2", "效率协同 > 业务中台 > 消息中心"});
        BUSINESS_LINE_MAP.put("667aaee5b93c1fa1fcf9daa5", new String[]{"邮件通知", "3", "效率协同 > 业务中台 > 消息中心 > 邮件通知"});
        BUSINESS_LINE_MAP.put("667aaef087e15fa070b1e1cf", new String[]{"飞书通知", "3", "效率协同 > 业务中台 > 消息中心 > 飞书通知"});
        BUSINESS_LINE_MAP.put("667aaef89cfb8c47fdbd6c0d", new String[]{"短信通知", "3", "效率协同 > 业务中台 > 消息中心 > 短信通知"});
        BUSINESS_LINE_MAP.put("667aaf03b93c1fa1fcf9daa6", new String[]{"系统通知", "3", "效率协同 > 业务中台 > 消息中心 > 系统通知"});
        BUSINESS_LINE_MAP.put("667aaed79cd4daf3f2236d8b", new String[]{"站内信", "4", "效率协同 > 业务中台 > 消息中心 > 系统通知 > 站内信"});
        BUSINESS_LINE_MAP.put("690af2b8557c1a4fbae35f31", new String[]{"系统更新通知", "4", "效率协同 > 业务中台 > 消息中心 > 系统通知 > 系统更新通知"});
        BUSINESS_LINE_MAP.put("690af2d44d83a8b161cc9e13", new String[]{"消息徽标及消息中心", "4", "效率协同 > 业务中台 > 消息中心 > 系统通知 > 消息徽标及消息中心"});
        BUSINESS_LINE_MAP.put("67b705857c2ebb91d54d986e", new String[]{"开放平台", "2", "效率协同 > 业务中台 > 开放平台"});
        BUSINESS_LINE_MAP.put("69413cf169d1bbf7e0737935", new String[]{"开放平台官网", "3", "效率协同 > 业务中台 > 开放平台 > 开放平台官网"});
        BUSINESS_LINE_MAP.put("69413d946bd4ff38582d4ef9", new String[]{"客户开放API需求", "3", "效率协同 > 业务中台 > 开放平台 > 客户开放API需求"});
        BUSINESS_LINE_MAP.put("694b5e5047e2674bb8c41a90", new String[]{"API 信息管理", "3", "效率协同 > 业务中台 > 开放平台 > API 信息管理"});
        BUSINESS_LINE_MAP.put("695233341246bdf84e70ba45", new String[]{"项目管理", "3", "效率协同 > 业务中台 > 开放平台 > 项目管理"});
        BUSINESS_LINE_MAP.put("667aaf95910c73c27d58e311", new String[]{"文件服务", "2", "效率协同 > 业务中台 > 文件服务"});
        BUSINESS_LINE_MAP.put("67455e64da77fd9be1d4b225", new String[]{"导出中心", "3", "效率协同 > 业务中台 > 文件服务 > 导出中心"});
        BUSINESS_LINE_MAP.put("67455e6a12214548a2f9b64a", new String[]{"云存储", "3", "效率协同 > 业务中台 > 文件服务 > 云存储"});
        BUSINESS_LINE_MAP.put("69670823966201d8d883d893", new String[]{"文件中心", "3", "效率协同 > 业务中台 > 文件服务 > 文件中心"});
        BUSINESS_LINE_MAP.put("667aafa487e15fa070b1e1d0", new String[]{"审计中心", "2", "效率协同 > 业务中台 > 审计中心"});
        BUSINESS_LINE_MAP.put("67455e0591973c7278adb66f", new String[]{"操作日志", "3", "效率协同 > 业务中台 > 审计中心 > 操作日志"});
        BUSINESS_LINE_MAP.put("690af984a09c434ec524feea", new String[]{"帮助中心", "2", "效率协同 > 业务中台 > 帮助中心"});
        BUSINESS_LINE_MAP.put("690af99bdbf7b6dd80fea08e", new String[]{"Feedback", "3", "效率协同 > 业务中台 > 帮助中心 > Feedback"});
        BUSINESS_LINE_MAP.put("690afa1182cb2691c52656d4", new String[]{"帮助文档", "3", "效率协同 > 业务中台 > 帮助中心 > 帮助文档"});
        BUSINESS_LINE_MAP.put("690afa1ef060bf15f4c57775", new String[]{"智能客服", "3", "效率协同 > 业务中台 > 帮助中心 > 智能客服"});
        BUSINESS_LINE_MAP.put("667aaf8a9cfb8c47fdbd6c0f", new String[]{"国际化平台", "2", "效率协同 > 业务中台 > 国际化平台"});
        BUSINESS_LINE_MAP.put("6679626d9cd4daf3f2236d79", new String[]{"SPOTTER 官网", "2", "效率协同 > 业务中台 > SPOTTER 官网"});
        BUSINESS_LINE_MAP.put("67455e3dbd9a9d14114f52bd", new String[]{"国内官网", "3", "效率协同 > 业务中台 > SPOTTER 官网 > 国内官网"});
        BUSINESS_LINE_MAP.put("67455e4491973c7278adb670", new String[]{"海外官网", "3", "效率协同 > 业务中台 > SPOTTER 官网 > 海外官网"});
        BUSINESS_LINE_MAP.put("67511055493bd3f39fae3ac8", new String[]{"业务工作台", "2", "效率协同 > 业务中台 > 业务工作台"});
        BUSINESS_LINE_MAP.put("6751106002f7d12d5f7e225a", new String[]{"Gmesh工作台", "3", "效率协同 > 业务中台 > 业务工作台 > Gmesh工作台"});
        BUSINESS_LINE_MAP.put("6751106a488816fb65c38b3d", new String[]{"SEVC工作台", "3", "效率协同 > 业务中台 > 业务工作台 > SEVC工作台"});
        BUSINESS_LINE_MAP.put("6942225bfc462716f83f3eaa", new String[]{"任务中心", "2", "效率协同 > 业务中台 > 任务中心"});
        BUSINESS_LINE_MAP.put("6679627db93c1fa1fcf9da93", new String[]{"技术中台", "1", "效率协同 > 技术中台"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669988", new String[]{"表单引擎", "2", "效率协同 > 技术中台 > 表单引擎"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669987", new String[]{"流程引擎", "2", "效率协同 > 技术中台 > 流程引擎"});
        BUSINESS_LINE_MAP.put("674562b2468ce1e37c86f733", new String[]{"流程配置", "3", "效率协同 > 技术中台 > 流程引擎 > 流程配置"});
        BUSINESS_LINE_MAP.put("667aafb7d7cd8f3ddc2f3608", new String[]{"定时调度", "2", "效率协同 > 技术中台 > 定时调度"});
        BUSINESS_LINE_MAP.put("667aafca9cd4daf3f2236d8c", new String[]{"研发能效洞察平台", "2", "效率协同 > 技术中台 > 研发能效洞察平台"});
        BUSINESS_LINE_MAP.put("667aafd7d430da01882188cb", new String[]{"一致性引擎", "2", "效率协同 > 技术中台 > 一致性引擎"});
        BUSINESS_LINE_MAP.put("667aafe39cfb8c47fdbd6c10", new String[]{"弱一致性-重试组件", "3", "效率协同 > 技术中台 > 一致性引擎 > 弱一致性-重试组件"});
        BUSINESS_LINE_MAP.put("667aafed8e134edc80856f94", new String[]{"强一致性-分布式事务", "3", "效率协同 > 技术中台 > 一致性引擎 > 强一致性-分布式事务"});
        BUSINESS_LINE_MAP.put("667aaffb6482393162aa1de7", new String[]{"智能助手", "2", "效率协同 > 技术中台 > 智能助手"});
        BUSINESS_LINE_MAP.put("667ab00ef2a33fb1197a5d72", new String[]{"业务助手", "3", "效率协同 > 技术中台 > 智能助手 > 业务助手"});
        BUSINESS_LINE_MAP.put("667ab017f2a33fb1197a5d73", new String[]{"研发助手", "3", "效率协同 > 技术中台 > 智能助手 > 研发助手"});
        BUSINESS_LINE_MAP.put("667ab01fe6f243593cbea511", new String[]{"客户助手", "3", "效率协同 > 技术中台 > 智能助手 > 客户助手"});
        BUSINESS_LINE_MAP.put("667ab041d7cd8f3ddc2f3609", new String[]{"技术博客建设", "2", "效率协同 > 技术中台 > 技术博客建设"});
        BUSINESS_LINE_MAP.put("667ab02ad430da01882188cc", new String[]{"技术氛围建设", "2", "效率协同 > 技术中台 > 技术氛围建设"});
        BUSINESS_LINE_MAP.put("690af9e7ddd12c2394256ca7", new String[]{"统一告警平台", "2", "效率协同 > 技术中台 > 统一告警平台"});
        BUSINESS_LINE_MAP.put("690afc008545434229e6665c", new String[]{"中间件", "2", "效率协同 > 技术中台 > 中间件"});
        BUSINESS_LINE_MAP.put("690afc09a0e8eb6bfbc67c56", new String[]{"SRE", "2", "效率协同 > 技术中台 > SRE"});
        BUSINESS_LINE_MAP.put("690afc144ebf6aadfba72524", new String[]{"二方包", "2", "效率协同 > 技术中台 > 二方包"});
        BUSINESS_LINE_MAP.put("694111451398832543623a62", new String[]{"规则引擎", "2", "效率协同 > 技术中台 > 规则引擎"});
        BUSINESS_LINE_MAP.put("68ea1b0c7f1f98bd533b7dc0", new String[]{"质量中台", "1", "效率协同 > 质量中台"});
        BUSINESS_LINE_MAP.put("6945111e379c73800208b8ac", new String[]{"AegisOnes", "2", "效率协同 > 质量中台 > AegisOnes"});
        BUSINESS_LINE_MAP.put("694511824c236f6cf0a9e01f", new String[]{"AegisSnap", "2", "效率协同 > 质量中台 > AegisSnap"});
        BUSINESS_LINE_MAP.put("694511b15ba13f58d7f248cd", new String[]{"AegisVigil", "2", "效率协同 > 质量中台 > AegisVigil"});
        BUSINESS_LINE_MAP.put("694511e94e8b124898143093", new String[]{"AegisEngine", "2", "效率协同 > 质量中台 > AegisEngine"});
        BUSINESS_LINE_MAP.put("667e2b7153c01d27d4d391d4", new String[]{"设计中台", "1", "效率协同 > 设计中台"});
        BUSINESS_LINE_MAP.put("668d02c11eeb6836676ef672", new String[]{"多渠道站点", "2", "效率协同 > 设计中台 > 多渠道站点"});
        BUSINESS_LINE_MAP.put("672ae77e8a93da022a2949d8", new String[]{"体验反馈", "2", "效率协同 > 设计中台 > 体验反馈"});
        BUSINESS_LINE_MAP.put("672ae78e078ee90bc36ce07b", new String[]{"UI&UX优化", "2", "效率协同 > 设计中台 > UI&UX优化"});
        BUSINESS_LINE_MAP.put("668e459208a5882c2d7a904a", new String[]{"内置页签", "3", "效率协同 > 设计中台 > UI&UX优化 > 内置页签"});
        BUSINESS_LINE_MAP.put("67455ebda0190c78b55b805f", new String[]{"一致性优化", "3", "效率协同 > 设计中台 > UI&UX优化 > 一致性优化"});
        BUSINESS_LINE_MAP.put("67455ed612214548a2f9b64b", new String[]{"UI 优化", "3", "效率协同 > 设计中台 > UI&UX优化 > UI 优化"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669990", new String[]{"供应链", "0", "供应链"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669992", new String[]{"订单履约", "1", "供应链 > 订单履约"});
        BUSINESS_LINE_MAP.put("667e2daea8b2528f0854b1fa", new String[]{"订单", "2", "供应链 > 订单履约 > 订单"});
        BUSINESS_LINE_MAP.put("667e2db871f771fe90517569", new String[]{"售后", "2", "供应链 > 订单履约 > 售后"});
        BUSINESS_LINE_MAP.put("667e2dc0d770169c39efbbc7", new String[]{"履约单", "2", "供应链 > 订单履约 > 履约单"});
        BUSINESS_LINE_MAP.put("667e2dc9a8b2528f0854b1fb", new String[]{"开放平台", "2", "供应链 > 订单履约 > 开放平台"});
        BUSINESS_LINE_MAP.put("667e2dd0a8b2528f0854b1fc", new String[]{"渠道库存", "2", "供应链 > 订单履约 > 渠道库存"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669993", new String[]{"仓储平台", "1", "供应链 > 仓储平台"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669994", new String[]{"出库", "2", "供应链 > 仓储平台 > 出库"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669995", new String[]{"库存", "2", "供应链 > 仓储平台 > 库存"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669996", new String[]{"入库", "2", "供应链 > 仓储平台 > 入库"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669997", new String[]{"增值服务", "2", "供应链 > 仓储平台 > 增值服务"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669998", new String[]{"基础配置", "2", "供应链 > 仓储平台 > 基础配置"});
        BUSINESS_LINE_MAP.put("667e2dee53c01d27d4d391d5", new String[]{"在库", "2", "供应链 > 仓储平台 > 在库"});
        BUSINESS_LINE_MAP.put("667e2df361ffc00a3d1ecf35", new String[]{"渠道对接", "2", "供应链 > 仓储平台 > 渠道对接"});
        BUSINESS_LINE_MAP.put("667e2dfdd770169c39efbbc8", new String[]{"货品", "2", "供应链 > 仓储平台 > 货品"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b669999", new String[]{"物流平台", "1", "供应链 > 物流平台"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66999a", new String[]{"物流单", "2", "供应链 > 物流平台 > 物流单"});
        BUSINESS_LINE_MAP.put("667e2e5453c01d27d4d391d6", new String[]{"头程", "2", "供应链 > 物流平台 > 头程"});
        BUSINESS_LINE_MAP.put("667e2e5a61ffc00a3d1ecf36", new String[]{"渠道对接", "2", "供应链 > 物流平台 > 渠道对接"});
        BUSINESS_LINE_MAP.put("667e2e63f6e41a490097fc10", new String[]{"亚马逊shipment", "2", "供应链 > 物流平台 > 亚马逊shipment"});
        BUSINESS_LINE_MAP.put("667e2e6b53c01d27d4d391d7", new String[]{"地址库", "2", "供应链 > 物流平台 > 地址库"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66999c", new String[]{"基础服务", "1", "供应链 > 基础服务"});
        BUSINESS_LINE_MAP.put("668e3632915d8e9adb90bf0c", new String[]{"其他", "1", "供应链 > 其他"});
        BUSINESS_LINE_MAP.put("668e363d8562e4d965daeae5", new String[]{"多渠道站点", "2", "供应链 > 其他 > 多渠道站点"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66999d", new String[]{"金融&财会", "0", "金融&财会"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66999e", new String[]{"VC结算", "1", "金融&财会 > VC结算"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a1", new String[]{"核对项目", "2", "金融&财会 > VC结算 > 核对项目"});
        BUSINESS_LINE_MAP.put("6891ce05e56f4345b6443abc", new String[]{"货款结算", "2", "金融&财会 > VC结算 > 货款结算"});
        BUSINESS_LINE_MAP.put("6891ce0f5145d70392e01722", new String[]{"VC应收结算", "2", "金融&财会 > VC结算 > VC应收结算"});
        BUSINESS_LINE_MAP.put("6891cec89425313eb021dcab", new String[]{"结算中心", "2", "金融&财会 > VC结算 > 结算中心"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b66999f", new String[]{"供应链结算", "1", "金融&财会 > 供应链结算"});
        BUSINESS_LINE_MAP.put("6891ccf1ca5b3ff7ae9a9b84", new String[]{"GDS结算", "2", "金融&财会 > 供应链结算 > GDS结算"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a0", new String[]{"票据", "2", "金融&财会 > 供应链结算 > 票据"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a3", new String[]{"财务凭证", "2", "金融&财会 > 供应链结算 > 财务凭证"});
        BUSINESS_LINE_MAP.put("6891ccdc5b9001e97123b754", new String[]{"核算", "2", "金融&财会 > 供应链结算 > 核算"});
        BUSINESS_LINE_MAP.put("689d94447ae23b08cdd100ab", new String[]{"税务", "2", "金融&财会 > 供应链结算 > 税务"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a2", new String[]{"金融支付", "1", "金融&财会 > 金融支付"});
        BUSINESS_LINE_MAP.put("6891c9e23b4123183b5012b8", new String[]{"金融", "2", "金融&财会 > 金融支付 > 金融"});
        BUSINESS_LINE_MAP.put("6891c9ee43dfb418a1555ca3", new String[]{"支付", "2", "金融&财会 > 金融支付 > 支付"});
        BUSINESS_LINE_MAP.put("6891cb133b4123183b5012b9", new String[]{"资金", "2", "金融&财会 > 金融支付 > 资金"});
        BUSINESS_LINE_MAP.put("67bd2a450ed75d2adedd6df5", new String[]{"广告", "2", "金融&财会 > 金融支付 > 广告"});
        BUSINESS_LINE_MAP.put("67e4c7c8ca498dd883e2af99", new String[]{"用户体验", "1", "金融&财会 > 用户体验"});
        BUSINESS_LINE_MAP.put("668e3649d64675ec01ee1f30", new String[]{"其他", "1", "金融&财会 > 其他"});
        BUSINESS_LINE_MAP.put("668e3652efa02940d694ef33", new String[]{"多渠道站点", "2", "金融&财会 > 其他 > 多渠道站点"});
        BUSINESS_LINE_MAP.put("68f73f6d3a029cd5339050b0", new String[]{"经营风控", "1", "金融&财会 > 经营风控"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a4", new String[]{"业务引擎", "0", "业务引擎"});
        BUSINESS_LINE_MAP.put("664db58b0eaf573a813809ba", new String[]{"数据服务", "1", "业务引擎 > 数据服务"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699ae", new String[]{"爬虫服务", "2", "业务引擎 > 数据服务 > 爬虫服务"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a8", new String[]{"数据加工", "2", "业务引擎 > 数据服务 > 数据加工"});
        BUSINESS_LINE_MAP.put("670f2ce768cbe79a768c3033", new String[]{"采集网关", "2", "业务引擎 > 数据服务 > 采集网关"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a9", new String[]{"采集引擎", "3", "业务引擎 > 数据服务 > 采集网关 > 采集引擎"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a5", new String[]{"业务网关", "3", "业务引擎 > 数据服务 > 采集网关 > 业务网关"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699a7", new String[]{"任务调度", "3", "业务引擎 > 数据服务 > 采集网关 > 任务调度"});
        BUSINESS_LINE_MAP.put("670f2d60103945977cfd49fb", new String[]{"渠道安全", "2", "业务引擎 > 数据服务 > 渠道安全"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699b1", new String[]{"渠道对接网关", "3", "业务引擎 > 数据服务 > 渠道安全 > 渠道对接网关"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699aa", new String[]{"认证维护", "3", "业务引擎 > 数据服务 > 渠道安全 > 认证维护"});
        BUSINESS_LINE_MAP.put("670f2ddc61fac8d10de2f1c7", new String[]{"数据监控", "2", "业务引擎 > 数据服务 > 数据监控"});
        BUSINESS_LINE_MAP.put("664db5997bca1e4bbd869bb1", new String[]{"数据平台", "1", "业务引擎 > 数据平台"});
        BUSINESS_LINE_MAP.put("664db68df89811d6fa2f8425", new String[]{"数据资产", "2", "业务引擎 > 数据平台 > 数据资产"});
        BUSINESS_LINE_MAP.put("663b579461f5181f8b6699b3", new String[]{"Dataverse", "2", "业务引擎 > 数据平台 > Dataverse"});
        BUSINESS_LINE_MAP.put("664db6c30eaf573a813809bb", new String[]{"数据服务", "2", "业务引擎 > 数据平台 > 数据服务"});
        BUSINESS_LINE_MAP.put("664db6d69bb4c2245bac8549", new String[]{"权限中心", "2", "业务引擎 > 数据平台 > 权限中心"});
        BUSINESS_LINE_MAP.put("664db6ef70fe6292e13f7eb1", new String[]{"指标库", "2", "业务引擎 > 数据平台 > 指标库"});
        BUSINESS_LINE_MAP.put("664dba5c9916ac16b571772b", new String[]{"报表中心", "2", "业务引擎 > 数据平台 > 报表中心"});
        BUSINESS_LINE_MAP.put("66cee8710e786fef07fd77e6", new String[]{"多维分析", "2", "业务引擎 > 数据平台 > 多维分析"});
        BUSINESS_LINE_MAP.put("670f398c103b940bc80a08e6", new String[]{"实时查询", "2", "业务引擎 > 数据平台 > 实时查询"});
        BUSINESS_LINE_MAP.put("684695a394fe4b2ec73b1e4d", new String[]{"数据标签", "2", "业务引擎 > 数据平台 > 数据标签"});
        BUSINESS_LINE_MAP.put("687df056136fbf8e1a4a1610", new String[]{"调度中心", "2", "业务引擎 > 数据平台 > 调度中心"});
        BUSINESS_LINE_MAP.put("68ca6acc94ae2b62d58d0b69", new String[]{"其他", "2", "业务引擎 > 数据平台 > 其他"});
        BUSINESS_LINE_MAP.put("68ca6ae238cc1480b7e139ab", new String[]{"导出中心", "3", "业务引擎 > 数据平台 > 其他 > 导出中心"});
        BUSINESS_LINE_MAP.put("6954ca3e4c01e6789e8d6501", new String[]{"指标监控", "2", "业务引擎 > 数据平台 > 指标监控"});
        BUSINESS_LINE_MAP.put("664db5abbc32a732de3390d9", new String[]{"数据开发", "1", "业务引擎 > 数据开发"});
        BUSINESS_LINE_MAP.put("670f2c5df18cfdfcbb24c915", new String[]{"数据工程", "2", "业务引擎 > 数据开发 > 数据工程"});
        BUSINESS_LINE_MAP.put("670f2e0a61fac8d10de2f1c8", new String[]{"数据处理", "3", "业务引擎 > 数据开发 > 数据工程 > 数据处理"});
        BUSINESS_LINE_MAP.put("670f2e55b4e13e8c64ef5e3b", new String[]{"数据架构", "3", "业务引擎 > 数据开发 > 数据工程 > 数据架构"});
        BUSINESS_LINE_MAP.put("670f2efdb93af2a38c2171e6", new String[]{"数据建模", "3", "业务引擎 > 数据开发 > 数据工程 > 数据建模"});
        BUSINESS_LINE_MAP.put("670f2c8cef4add931b85c7b5", new String[]{"数据治理", "2", "业务引擎 > 数据开发 > 数据治理"});
        BUSINESS_LINE_MAP.put("670f2f2d7f13f5759325432e", new String[]{"数据标准", "3", "业务引擎 > 数据开发 > 数据治理 > 数据标准"});
        BUSINESS_LINE_MAP.put("670f2f3c61fac8d10de2f1c9", new String[]{"数据安全", "3", "业务引擎 > 数据开发 > 数据治理 > 数据安全"});
        BUSINESS_LINE_MAP.put("670f2f4cd4a5f6325ae3e7a1", new String[]{"数据质量", "3", "业务引擎 > 数据开发 > 数据治理 > 数据质量"});
        BUSINESS_LINE_MAP.put("670f2c97238ff10b3ea01eea", new String[]{"数据智能", "2", "业务引擎 > 数据开发 > 数据智能"});
        BUSINESS_LINE_MAP.put("670f2f65c7a7f792c068f968", new String[]{"算法工程", "3", "业务引擎 > 数据开发 > 数据智能 > 算法工程"});
        BUSINESS_LINE_MAP.put("670f2f8401c564719fcb3fd2", new String[]{"AI", "3", "业务引擎 > 数据开发 > 数据智能 > AI"});
        BUSINESS_LINE_MAP.put("668e3661aa9a5339a6c6db7a", new String[]{"其他", "1", "业务引擎 > 其他"});
        BUSINESS_LINE_MAP.put("668e366a08a5882c2d7a9048", new String[]{"多渠道站点", "2", "业务引擎 > 其他 > 多渠道站点"});
        BUSINESS_LINE_MAP.put("69831b1a79dd5d312a67e519", new String[]{"风控中台", "1", "业务引擎 > 风控中台"});
        BUSINESS_LINE_MAP.put("668277d017cbe26cfe50d5aa", new String[]{"数据分析", "0", "数据分析"});
        BUSINESS_LINE_MAP.put("66827e20d425574c73b3ebe3", new String[]{"经营报告", "1", "数据分析 > 经营报告"});
        BUSINESS_LINE_MAP.put("6683792bd465b906fe01a09e", new String[]{"营收与利润", "2", "数据分析 > 经营报告 > 营收与利润"});
        BUSINESS_LINE_MAP.put("66827e50c6ddb5cfff3e6c77", new String[]{"订单", "2", "数据分析 > 经营报告 > 订单"});
        BUSINESS_LINE_MAP.put("66827f69399039addc5fc93a", new String[]{"业务分析", "2", "数据分析 > 经营报告 > 业务分析"});
        BUSINESS_LINE_MAP.put("66827f88a957502f0caa1940", new String[]{"商家管理", "1", "数据分析 > 商家管理"});
        BUSINESS_LINE_MAP.put("66827f9ca957502f0caa1941", new String[]{"供应商", "2", "数据分析 > 商家管理 > 供应商"});
        BUSINESS_LINE_MAP.put("66827fb8a957502f0caa1942", new String[]{"产品", "2", "数据分析 > 商家管理 > 产品"});
        BUSINESS_LINE_MAP.put("668280aa599e426e8912e25f", new String[]{"渠道表现", "1", "数据分析 > 渠道表现"});
        BUSINESS_LINE_MAP.put("66828123c6ddb5cfff3e6c78", new String[]{"Sales", "2", "数据分析 > 渠道表现 > Sales"});
        BUSINESS_LINE_MAP.put("66828144a957502f0caa1943", new String[]{"Forecasting", "2", "数据分析 > 渠道表现 > Forecasting"});
        BUSINESS_LINE_MAP.put("6682815586102e4136eda6a2", new String[]{"促销活动", "2", "数据分析 > 渠道表现 > 促销活动"});
        BUSINESS_LINE_MAP.put("6682816286102e4136eda6a3", new String[]{"广告", "2", "数据分析 > 渠道表现 > 广告"});
        BUSINESS_LINE_MAP.put("6682817b17cbe26cfe50d5b1", new String[]{"财务", "1", "数据分析 > 财务"});
        BUSINESS_LINE_MAP.put("66828191a957502f0caa1944", new String[]{"成本分析", "2", "数据分析 > 财务 > 成本分析"});
        BUSINESS_LINE_MAP.put("668281b0d465b906fe01a088", new String[]{"渠道发票", "2", "数据分析 > 财务 > 渠道发票"});
        BUSINESS_LINE_MAP.put("668281c717cbe26cfe50d5b2", new String[]{"供应链费用", "2", "数据分析 > 财务 > 供应链费用"});
        BUSINESS_LINE_MAP.put("66837a698e29809b1da51973", new String[]{"金融风控", "2", "数据分析 > 财务 > 金融风控"});
        BUSINESS_LINE_MAP.put("68fadfbe35c8b19f4a6f588e", new String[]{"RSE分析", "1", "数据分析 > RSE分析"});
        BUSINESS_LINE_MAP.put("68fae055747c1738b343aa15", new String[]{"合规&运营", "2", "数据分析 > RSE分析 > 合规&运营"});
        BUSINESS_LINE_MAP.put("68fae068bd1bb28c9932de8e", new String[]{"渠道&促销", "2", "数据分析 > RSE分析 > 渠道&促销"});
        BUSINESS_LINE_MAP.put("68fae078aca043a2ae53eb81", new String[]{"CRM", "2", "数据分析 > RSE分析 > CRM"});
        BUSINESS_LINE_MAP.put("68fadfe2da943f90c0ec594f", new String[]{"GDS分析", "1", "数据分析 > GDS分析"});
        BUSINESS_LINE_MAP.put("66827fe017cbe26cfe50d5af", new String[]{"仓储", "2", "数据分析 > GDS分析 > 仓储"});
        BUSINESS_LINE_MAP.put("66827ff1a93d2c24218dbe1b", new String[]{"出入库", "3", "数据分析 > GDS分析 > 仓储 > 出入库"});
        BUSINESS_LINE_MAP.put("668280111ebcbb8b75cc8ce0", new String[]{"库存", "3", "数据分析 > GDS分析 > 仓储 > 库存"});
        BUSINESS_LINE_MAP.put("6682802817cbe26cfe50d5b0", new String[]{"库内运作", "3", "数据分析 > GDS分析 > 仓储 > 库内运作"});
        BUSINESS_LINE_MAP.put("6682803c86102e4136eda6a1", new String[]{"物流", "2", "数据分析 > GDS分析 > 物流"});
        BUSINESS_LINE_MAP.put("668280501ebcbb8b75cc8ce1", new String[]{"SPT物流", "3", "数据分析 > GDS分析 > 物流 > SPT物流"});
        BUSINESS_LINE_MAP.put("66828065641b6d8b1e3b015f", new String[]{"VC物流", "3", "数据分析 > GDS分析 > 物流 > VC物流"});
        BUSINESS_LINE_MAP.put("66828079599e426e8912e25e", new String[]{"尾程物流", "3", "数据分析 > GDS分析 > 物流 > 尾程物流"});
        BUSINESS_LINE_MAP.put("68fadffe3af5064e5896e24e", new String[]{"PLUT分析", "1", "数据分析 > PLUT分析"});
        BUSINESS_LINE_MAP.put("68fae09776a8978f72cbcf49", new String[]{"金融业务", "2", "数据分析 > PLUT分析 > 金融业务"});
        BUSINESS_LINE_MAP.put("68fae0bb583e9a26817d39cc", new String[]{"贸易&回款", "2", "数据分析 > PLUT分析 > 贸易&回款"});
        BUSINESS_LINE_MAP.put("68fae0c9747c1738b343aa16", new String[]{"风控", "2", "数据分析 > PLUT分析 > 风控"});
        BUSINESS_LINE_MAP.put("68fae86a81fac2cffb27ef2d", new String[]{"产品运营", "1", "数据分析 > 产品运营"});
        BUSINESS_LINE_MAP.put("69a4f1465972c244ecae5cc6", new String[]{"业务分析", "1", "数据分析 > 业务分析"});
        BUSINESS_LINE_MAP.put("69a4f176442de6e46949aac1", new String[]{"分析工程", "1", "数据分析 > 分析工程"});
        BUSINESS_LINE_MAP.put("69a4f24c4a428d9c3853b13e", new String[]{"产品运营", "2", "数据分析 > 分析工程 > 产品运营"});
        BUSINESS_LINE_MAP.put("69a4f28e04e372357ecc8262", new String[]{"库存与物流", "2", "数据分析 > 分析工程 > 库存与物流"});
        BUSINESS_LINE_MAP.put("69a4f2a15972c244ecae5cc7", new String[]{"渠道与合规", "2", "数据分析 > 分析工程 > 渠道与合规"});
        BUSINESS_LINE_MAP.put("69a4f2e0c185918a655c2e04", new String[]{"价值与利润", "2", "数据分析 > 分析工程 > 价值与利润"});
        BUSINESS_LINE_MAP.put("669e28e296999530e4eedab8", new String[]{"DTC", "0", "DTC"});
    }

    /** raw（id 或中文 name）→ 飞书业务线 id。 */
    public static String toBusinessLineId(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String t = raw.trim();
        if (BUSINESS_LINE_MAP.containsKey(t)) return t;
        for (java.util.Map.Entry<String, String[]> e : BUSINESS_LINE_MAP.entrySet()) {
            if (e.getValue()[0].equals(t)) return e.getKey();
        }
        return t;
    }

    /** 业务线选项列表（写死），供前端下拉。每项为 {id, name, level, path}，严格按映射表顺序。 */
    public static java.util.List<java.util.Map<String, String>> getBusinessLineOptionsStatic() {
        java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, String[]> e : BUSINESS_LINE_MAP.entrySet()) {
            String[] v = e.getValue();
            list.add(java.util.Map.of("id", e.getKey(), "name", v[0], "level", v[1], "path", v[2]));
        }
        return list;
    }
}
