package io.vanguard.testops;

import io.vanguard.testops.api.runtime.config.JmeterProperties;
import io.vanguard.testops.system.infrastructure.storage.config.OssProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude = {
        QuartzAutoConfiguration.class,
        LdapAutoConfiguration.class,
        Neo4jAutoConfiguration.class
})
@PropertySource(value = {
        "classpath:commons.properties",
        "file:${user.dir}/opt/metersphere/conf/metersphere.properties",
        "file:/opt/metersphere/conf/metersphere.properties",
}, encoding = "UTF-8", ignoreResourceNotFound = true)
@ServletComponentScan
@ComponentScan(basePackages = {
        "io.vanguard.testops.config",
        "io.vanguard.testops.controller",
        "io.vanguard.testops.sdk",
        "io.vanguard.testops.system",
        "io.vanguard.testops.project",
        "io.vanguard.testops.api",
        "io.vanguard.testops.plan",
        "io.vanguard.testops.bug",
        "io.vanguard.testops.functional",
        "io.vanguard.testops.dashboard",
        "io.vanguard.testops.requirementquality",
        "io.vanguard.testops.workflow",
        "io.vanguard.testops.metadata",
        "io.vanguard.testops.listener"
})
@EnableConfigurationProperties({
        OssProperties.class,
        JmeterProperties.class
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
