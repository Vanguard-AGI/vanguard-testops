package io.vanguard.testops.system.config;

import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.impl.DefaultUidGenerator;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartListener implements ApplicationRunner {

    @Resource
    private DefaultUidGenerator defaultUidGenerator;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LogUtils.info("================= 应用启动 =================");
        defaultUidGenerator.init();
    }
}
