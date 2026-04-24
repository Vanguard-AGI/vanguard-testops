package io.vanguard.testops.system.security.aspect;

import io.vanguard.testops.plugin.sdk.spi.QuotaPlugin;
import io.vanguard.testops.system.service.PluginLoadService;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class QuotaInterceptor {
    @Resource
    private PluginLoadService pluginLoadService;
    // 插件ID
    private final String QUOTA = "cloud-quota-plugin";

    /*@Around("execution(* io.vanguard.testops..*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) ||" +
            "@annotation(org.springframework.web.bind.annotation.GetMapping)|| " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping))")*/
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        // 验证配额规则
        PluginWrapper pluginWrapper = pluginLoadService.getMsPluginManager().getPlugin(QUOTA);
        if (pluginWrapper != null && pluginWrapper.getPlugin() instanceof QuotaPlugin quotaPlugin) {
            quotaPlugin.interceptor(pjp);
        }
        return pjp.proceed();
    }
}
