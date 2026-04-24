package io.vanguard.testops.system.infrastructure.storage.config;


import io.vanguard.testops.sdk.constants.DefaultRepositoryDir;
import io.vanguard.testops.sdk.file.FileCenter;
import io.vanguard.testops.sdk.file.FileRepository;
import io.vanguard.testops.sdk.file.FileRequest;
import io.vanguard.testops.sdk.file.OssRepository;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.RsaKey;
import io.vanguard.testops.sdk.util.RsaUtils;
import io.minio.MinioClient;
import com.aliyun.oss.OSS;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsaConfig implements ApplicationRunner {
    @Resource(name = "ossClient")
    private Object client;
    
    @Resource
    private OssProperties ossProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        FileRequest request = new FileRequest();
        request.setFileName("rsa.key");
        request.setFolder(DefaultRepositoryDir.getSystemRootDir());
        FileRepository fileRepository = FileCenter.getDefaultRepository();
        // 初始化OSS配置（兼容 MinIO 与阿里云 OSS，优先使用OSS）
        if (client instanceof OSS) {
            ((OssRepository) fileRepository).init((OSS) client, ossProperties.getBucketName());
        } else if (client instanceof MinioClient) {
            ((OssRepository) fileRepository).init((MinioClient) client, ossProperties.getBucketName());
        }

        try {
            byte[] file = fileRepository.getFile(request);
            if (file != null) {
                RsaKey rsaKey = SerializationUtils.deserialize(file);
                RsaUtils.setRsaKey(rsaKey);
                return;
            }
        } catch (Exception ignored) {
        }
        // 保存到OSS
        RsaKey rsaKey = RsaUtils.getRsaKey();
        byte[] bytes = SerializationUtils.serialize(rsaKey);
        fileRepository.saveFile(bytes, request);
        RsaUtils.setRsaKey(rsaKey);
    }
}
