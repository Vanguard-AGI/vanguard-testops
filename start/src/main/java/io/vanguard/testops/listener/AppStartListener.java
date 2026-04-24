package io.vanguard.testops.listener;

import io.vanguard.testops.functional.domain.ExportTask;
import io.vanguard.testops.functional.domain.ExportTaskExample;
import io.vanguard.testops.functional.mapper.ExportTaskMapper;
import io.vanguard.testops.sdk.constants.StorageType;
import io.vanguard.testops.sdk.file.FileCenter;
import io.vanguard.testops.sdk.file.OssRepository;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.constants.ExportConstants;
import io.vanguard.testops.system.infrastructure.storage.config.OssProperties;
import io.vanguard.testops.system.service.BaseScheduleService;
import io.vanguard.testops.system.service.PluginLoadService;
import io.vanguard.testops.system.uid.impl.DefaultUidGenerator;
import io.minio.MinioClient;
import com.aliyun.oss.OSS;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartListener implements ApplicationRunner {

    @Resource
    private PluginLoadService pluginLoadService;
    @Resource(name = "ossClient")
    private Object ossClient;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private BaseScheduleService baseScheduleService;

    @Resource
    private DefaultUidGenerator defaultUidGenerator;

    @Resource
    private ExportTaskMapper exportTaskMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LogUtils.info("================= 应用启动 =================");
        // 初始化 UID 生成器（必须在 MyBatis 完全初始化之后）
        defaultUidGenerator.init();
        
        // 初始化文件存储（兼容 MinIO 与阿里云 OSS，优先使用OSS）
        LogUtils.info("开始初始化文件存储，ossClient类型: {}", ossClient != null ? ossClient.getClass().getSimpleName() : "null");
        LogUtils.info("OSS配置: endpoint={}, bucketName={}", ossProperties.getEndpoint(), ossProperties.getBucketName());

        try {
            if (ossClient instanceof OSS) {
                LogUtils.info("检测到阿里云OSS客户端，使用阿里云OSS初始化");
                ((OssRepository) FileCenter.getRepository(StorageType.OSS)).init((OSS) ossClient, ossProperties.getBucketName());
            } else if (ossClient instanceof MinioClient) {
                LogUtils.info("检测到MinioClient，使用MinIO兼容模式初始化");
                ((OssRepository) FileCenter.getRepository(StorageType.OSS)).init((MinioClient) ossClient, ossProperties.getBucketName());
            } else {
                LogUtils.warn("未知的OSS客户端类型: {}", ossClient != null ? ossClient.getClass().getName() : "null");
            }
            LogUtils.info("文件存储初始化完成");
        } catch (Exception e) {
            LogUtils.error("文件存储初始化失败，跳过初始化: {}", e.getMessage());
        }

        LogUtils.info("初始化定时任务");
        baseScheduleService.startEnableSchedules();

        LogUtils.info("初始化导出未完成任务的状态");
        ExportTaskExample exportTaskExample = new ExportTaskExample();
        exportTaskExample.createCriteria().andStateEqualTo(ExportConstants.ExportState.PREPARED.name());
        ExportTask exportTask = new ExportTask();
        exportTask.setState(ExportConstants.ExportState.STOP.name());
        exportTaskMapper.updateByExampleSelective(exportTask, exportTaskExample);
        // 加载插件
        pluginLoadService.loadPlugins();
    }
}
