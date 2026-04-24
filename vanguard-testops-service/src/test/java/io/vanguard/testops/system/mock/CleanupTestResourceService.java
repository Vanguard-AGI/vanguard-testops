package io.vanguard.testops.system.mock;

import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import org.springframework.stereotype.Component;

@Component
public class CleanupTestResourceService implements CleanupProjectResourceService {

    @Override
    public void deleteResources(String projectId) {
        LogUtils.info("删除当前项目[" + projectId + "]TEST资源");
    }

}
