package io.vanguard.testops.system.infrastructure.cache.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.redisson.file:}")
    private String redissonConfigFile;

    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:1}")
    private int redisDatabase;

    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redisson() throws IOException {
        Config config;
        
        if (redissonConfigFile != null && !redissonConfigFile.isEmpty()) {
            // 处理 Windows 路径问题
            String normalizedPath = normalizePath(redissonConfigFile);
            File configFile = new File(normalizedPath);
            
            if (configFile.exists() && configFile.isFile()) {
                // 使用文件路径加载配置
                try (InputStream inputStream = new FileInputStream(configFile)) {
                    config = Config.fromYAML(inputStream);
                }
            } else {
                // 如果配置文件不存在，使用环境变量配置
                config = createConfigFromProperties();
            }
        } else {
            // 如果没有指定配置文件，使用环境变量配置
            config = createConfigFromProperties();
        }
        
        return Redisson.create(config);
    }

    /**
     * 规范化路径，处理 Windows 和 Linux 路径差异
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // 移除 file:/// 或 file: 前缀
        path = path.replaceFirst("^file:///", "").replaceFirst("^file:", "");
        
        // 处理 ${user.dir} 变量
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            path = path.replace("${user.dir}", userDir);
        }
        
        // 在 Windows 上，将正斜杠转换为反斜杠（如果需要）
        // 但 Redisson 的 YAML 解析器应该能处理正斜杠
        // 所以这里保持原样
        
        return path;
    }

    /**
     * 从 Spring Boot 配置属性创建 Redisson 配置
     */
    private Config createConfigFromProperties() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase);
        
        // 如果配置了密码，则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return config;
    }
}
