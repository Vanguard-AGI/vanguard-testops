package io.vanguard.testops.system.infrastructure.storage.config;

import io.vanguard.testops.sdk.util.LogUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.Bucket;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(prefix = "oss", name = "endpoint")
public class OssConfig {

    public static final String DEFAULT_BUCKET = "spotter-aegis-test";

    @Bean(name = "ossClient")
    @Primary
    public Object ossClient(OssProperties ossProperties) throws Exception {
        LogUtils.info("初始化对象存储客户端，端点: {}", ossProperties.getEndpoint());

        String bucketName = StringUtils.isNotBlank(ossProperties.getBucketName()) ?
                ossProperties.getBucketName() : DEFAULT_BUCKET;

        String provider = StringUtils.defaultIfBlank(ossProperties.getProvider(), "");
        boolean isAliyun = StringUtils.containsIgnoreCase(ossProperties.getEndpoint(), "aliyuncs.com")
                || StringUtils.equalsIgnoreCase(provider, "aliyun");

        if (isAliyun) {
            // 使用阿里云 OSS SDK
            OSS client = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKey(),
                    ossProperties.getSecretKey());
            try {
                boolean exist = client.doesBucketExist(bucketName);
                if (!exist) {
                    LogUtils.info("存储桶 {} 不存在，正在创建...", bucketName);
                    client.createBucket(bucketName);
                    LogUtils.info("存储桶 {} 创建成功", bucketName);
                } else {
                    LogUtils.info("存储桶 {} 已存在", bucketName);
                }
            } catch (Exception e) {
                LogUtils.error("初始化阿里云 OSS 存储桶失败", e);
                throw e;
            }
            LogUtils.info("阿里云 OSS 客户端初始化完成");
            return client;
        } else {
            // 默认 MinIO/S3 兼容
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(ossProperties.getEndpoint())
                    .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey());
            if (StringUtils.isNotBlank(ossProperties.getRegion())) {
                builder.region(ossProperties.getRegion());
            }
            MinioClient client = builder.build();
            try {
                boolean exist = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!exist) {
                    LogUtils.info("存储桶 {} 不存在，正在创建...", bucketName);
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    LogUtils.info("存储桶 {} 创建成功", bucketName);
                } else {
                    LogUtils.info("存储桶 {} 已存在", bucketName);
                }
            } catch (Exception e) {
                LogUtils.error("初始化 MinIO 存储桶失败", e);
                throw e;
            }
            LogUtils.info("MinIO 客户端初始化完成");
            return client;
        }
    }
}

