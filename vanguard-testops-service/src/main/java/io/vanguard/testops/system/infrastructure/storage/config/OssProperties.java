package io.vanguard.testops.system.infrastructure.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = OssProperties.OSS_PREFIX)
@Getter
@Setter
public class OssProperties {
    public static final String OSS_PREFIX = "oss";

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName = "spotter-aegis-test";
    private boolean pathStyleAccess = false;
    private String region;

    /**
     * 指定提供商，可选：minio、aliyun
     * 为空时根据 endpoint 简单判断（包含 oss-.*.aliyuncs.com 视为 aliyun）
     */
    private String provider;
}

