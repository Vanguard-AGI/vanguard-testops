package io.vanguard.testops.sdk.util;

import java.util.HashMap;
import java.util.Map;

public class FilterChainUtils {

    public static Map<String, String> loadBaseFilterChain() {
        Map<String, String> filterChainDefinitionMap = new HashMap<>();
        filterChainDefinitionMap.put("/*.html", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/ldap/login", "anon");
        filterChainDefinitionMap.put("/authentication/get-list", "anon");
        filterChainDefinitionMap.put("/authentication/get/by/type/**", "anon");
        filterChainDefinitionMap.put("/we_com/info", "anon");
        filterChainDefinitionMap.put("/ding_talk/info", "anon");
        filterChainDefinitionMap.put("/lark/info", "anon");
        filterChainDefinitionMap.put("/lark_suite/info", "anon");
        filterChainDefinitionMap.put("/lark/info/with_detail", "anon");
        filterChainDefinitionMap.put("/lark/login", "anon");
        filterChainDefinitionMap.put("/lark/user", "anon");
        filterChainDefinitionMap.put("/lark/save", "anon");
        filterChainDefinitionMap.put("/lark/enable", "anon");
        filterChainDefinitionMap.put("/lark/validate", "anon");
        filterChainDefinitionMap.put("/lark/change/validate", "anon");
        filterChainDefinitionMap.put("/sso/callback/we_com", "anon");
        filterChainDefinitionMap.put("/sso/callback/ding_talk", "anon");
        filterChainDefinitionMap.put("/sso/callback/lark", "anon");
        filterChainDefinitionMap.put("/sso/callback/lark_suite", "anon");
        filterChainDefinitionMap.put("/devops/feishu/callback", "anon"); // 更新为新的回调路径
        filterChainDefinitionMap.put("/api/callback/meego", "anon"); // 飞书Meego Webhook回调
        filterChainDefinitionMap.put("/api/callback/meego/sync", "anon"); // 飞书Meego 手动同步
        filterChainDefinitionMap.put("/api/callback/meego/stats", "anon"); // 飞书Meego 统计查询
        filterChainDefinitionMap.put("/setting/get/platform/param", "anon");
        filterChainDefinitionMap.put("/signout", "anon");
        filterChainDefinitionMap.put("/is-login", "anon");
        filterChainDefinitionMap.put("/get-key", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/images/**", "anon");
        filterChainDefinitionMap.put("/assets/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/display/info", "anon");
        filterChainDefinitionMap.put("/file/preview/**", "anon");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/base-display/**", "anon");
        filterChainDefinitionMap.put("/jmeter/ping", "anon");
        filterChainDefinitionMap.put("/authsource/list/allenable", "anon");
        filterChainDefinitionMap.put("/sso/callback/**", "anon");
        filterChainDefinitionMap.put("/license/validate", "anon");
        //mock-server
        filterChainDefinitionMap.put("/mock-server/**", "anon");

        //功能用例富文本访问
        filterChainDefinitionMap.put("/attachment/download/file/**", "anon");
        //用例评审富文本访问
        filterChainDefinitionMap.put("/review/functional/case/download/file/**", "anon");
        //缺陷管理富文本访问
        filterChainDefinitionMap.put("/bug/attachment/preview/md/**", "anon");
        //计划报告富文本访问
        filterChainDefinitionMap.put("/test-plan/report/preview/md/**", "anon");
        //模板富文本框图片预览
        filterChainDefinitionMap.put("/organization/template/img/preview/**", "anon");
        filterChainDefinitionMap.put("/project/template/img/preview/**", "anon");

        filterChainDefinitionMap.put("/system/version/current", "anon");

        //用户通过邮箱邀请自行注册的接口
        filterChainDefinitionMap.put("/system/user/check-invite/**", "anon");
        filterChainDefinitionMap.put("/system/user/register-by-invite", "anon");

        // 下载测试资源
        filterChainDefinitionMap.put("/api/execute/resource/**", "anon");

        // ========== AegisOne Web 效能数据大屏接口（新版Dashboard API）==========
        
        // 1. 新版效能仪表盘聚合接口（包含所有子接口）
        filterChainDefinitionMap.put("/metrics/dashboard/**", "anon");
        // 具体包含的接口：
        //   - /metrics/dashboard/project-overview - 项目概览指标
        //   - /metrics/dashboard/personal-stats - 个人统计指标
        //   - /metrics/dashboard/change-reason-distribution - 用例变更原因分布统计
        //   - /metrics/dashboard/blocked-reason-distribution - 用例执行阻塞原因分布统计

        // 效率仪数据大盘（免鉴权）
        filterChainDefinitionMap.put("/metrics/efficiency/**", "anon");
        //   - /metrics/efficiency/overview - 效率仪概览
        //   - /metrics/efficiency/activity - 用户活跃度

        // 需求质量视图（与效能数据大屏一致：免鉴权，供工作台大屏访问）
        filterChainDefinitionMap.put("/metrics/requirement-quality/**", "anon");
        // 具体包含：/list、/overview、/detail/{id}、/filter-options、/story-search、/pipeline/report、/pipeline/list、/pipeline/update、/pipeline/create

        // 2. 用例列表接口（点击指标查看详情用）
        filterChainDefinitionMap.put("/functional/case/metrics/case-list", "anon");
        
        // 3. CS值批量计算公开接口（用于后台任务和数据初始化）
        filterChainDefinitionMap.put("/functional/case/metrics/cs/batch-calculate/public", "anon");
        
        // 4. 测试计划指标计算公开接口（用于后台任务和数据初始化）
        filterChainDefinitionMap.put("/test-plan/metrics/test-plan/calculate/public", "anon");
        
        // 5. 项目列表公开接口（项目选择器）
        filterChainDefinitionMap.put("/project/list/public", "anon");
        
        // 6. 用户列表公开接口（用户选择器）
        filterChainDefinitionMap.put("/system/user/list/public", "anon");

        // for swagger
        filterChainDefinitionMap.put("/swagger-ui.html", "anon");
        filterChainDefinitionMap.put("/swagger-ui/**", "anon");
        filterChainDefinitionMap.put("/v3/api-docs/**", "anon");

        filterChainDefinitionMap.put("/403", "anon");
        filterChainDefinitionMap.put("/anonymous/**", "anon");

        //分享相关接口
        filterChainDefinitionMap.put("/api/share/doc/view/**", "anon");

        filterChainDefinitionMap.put("/system/theme", "anon");
        filterChainDefinitionMap.put("/system/parameter/save/base-url/**", "anon");
        filterChainDefinitionMap.put("/system/timeout", "anon");
        filterChainDefinitionMap.put("/file/metadata/info/**", "anon");
        // consul
        filterChainDefinitionMap.put("/v1/catalog/**", "anon");
        filterChainDefinitionMap.put("/v1/agent/**", "anon");
        filterChainDefinitionMap.put("/v1/health/**", "anon");
        //mock接口
        filterChainDefinitionMap.put("/mock/**", "anon");
        filterChainDefinitionMap.put("/ws/**", "anon");
        //
        filterChainDefinitionMap.put("/performance/update/cache", "anon");
        // websocket
        filterChainDefinitionMap.put("/websocket/**", "csrf");

        // 获取插件中的图片
        filterChainDefinitionMap.put("/plugin/image/**", "anon");
        filterChainDefinitionMap.put("/templates/user_import_en.xlsx", "anon");
        filterChainDefinitionMap.put("/templates/user_import_cn.xlsx", "anon");

        //分享报告接口
        filterChainDefinitionMap.put("/api/report/case/share/**", "anon");
        filterChainDefinitionMap.put("/api/report/scenario/share/**", "anon");
        filterChainDefinitionMap.put("/api/report/share/get/**", "anon");
        // 测试计划报告分享接口
        filterChainDefinitionMap.put("/test-plan/report/share/detail/**", "anon");
        filterChainDefinitionMap.put("/test-plan/report/share/get/**", "anon");
        filterChainDefinitionMap.put("/test-plan/report/share/get-layout/**", "anon");
        // 默认语言
        filterChainDefinitionMap.put("/user/local/config/default-locale", "anon");
        // 定义-分享
        filterChainDefinitionMap.put("/api/doc/share/detail/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/get-detail/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/check/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/module/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/export/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/stop/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/download/file/**", "anon");
        filterChainDefinitionMap.put("/api/doc/share/plugin/script/**", "anon");

        // 元数据定义接口 - 暂时去掉鉴权
        filterChainDefinitionMap.put("/metadata/definition/**", "anon");
        // 元数据模块接口 - 暂时去掉鉴权
        filterChainDefinitionMap.put("/metadata/module/**", "anon");
        // 环境配置接口 - 暂时去掉鉴权
        filterChainDefinitionMap.put("/user/profile/**", "anon");

        // 工作流执行机回调接口（执行机调用，无需鉴权）
        filterChainDefinitionMap.put("/workflow/run/result/callback", "anon");


        // Aegis 同步接口（外部插件调用，无需鉴权）
        filterChainDefinitionMap.put("/api/aegis/sync", "anon");

        // 飞书缺陷 Webhook 回调（飞书服务器调用，无需鉴权）
        filterChainDefinitionMap.put("/webhook/feishu/**", "anon");

        return filterChainDefinitionMap;
    }

    public static Map<String, String> ignoreCsrfFilter() {
        Map<String, String> filterChainDefinitionMap = new HashMap<>();
        filterChainDefinitionMap.put("/", "apikey, authc"); // 跳转到 / 不用校验 csrf
        filterChainDefinitionMap.put("/language", "apikey, authc");// 跳转到 /language 不用校验 csrf
        filterChainDefinitionMap.put("/mock", "apikey, authc"); // 跳转到 /mock接口 不用校验 csrf
        return filterChainDefinitionMap;
    }

}
