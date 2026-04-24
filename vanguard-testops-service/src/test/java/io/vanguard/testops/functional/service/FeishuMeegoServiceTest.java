package io.vanguard.testops.functional.service;

// import io.vanguard.testops.functional.domain.MeegoProjectStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 飞书Meego服务测试类
 * 注意：这些测试需要真实的飞书环境和配置才能运行
 */
// @SpringBootTest
public class FeishuMeegoServiceTest {

    // @Autowired
    private FeishuMeegoService feishuMeegoService;

/*
    // @Test
    public void testGetPluginToken() {
        String token = feishuMeegoService.getPluginToken();
        System.out.println("Plugin Token: " + token);
        assert token != null && !token.isEmpty();
    }

    // @Test
    public void testGetUserKey() {
        String openId = "ou_xxx"; // 替换为真实的OpenID
        String userKey = feishuMeegoService.getUserKey(openId);
        System.out.println("User Key: " + userKey);
        assert userKey != null && !userKey.isEmpty();
    }

    // @Test
    public void testSyncProjects() {
        String userOpenId = "ou_xxx"; // 替换为真实的OpenID
        feishuMeegoService.syncProjects(userOpenId);
        
        // 验证数据是否同步成功
        List<MeegoProjectStats> stats = feishuMeegoService.getProjectStatsList();
        System.out.println("同步了 " + stats.size() + " 个项目");
        
        stats.forEach(stat -> {
            System.out.println(String.format("项目: %s, 缺陷数: %d", 
                stat.getProjectName(), stat.getDefectCount()));
        });
    }

    // @Test
    public void testUpdateProjectDefectCount() {
        String projectKey = "PROJECT_001"; // 替换为真实的项目Key
        String userOpenId = "ou_xxx"; // 替换为真实的OpenID
        
        feishuMeegoService.updateProjectDefectCount(projectKey, userOpenId);
        System.out.println("项目缺陷数量更新完成");
    }

    // @Test
    public void testGetProjectStatsList() {
        List<MeegoProjectStats> stats = feishuMeegoService.getProjectStatsList();
        
        System.out.println("共有 " + stats.size() + " 个项目");
        stats.forEach(stat -> {
            System.out.println(String.format(
                "ID: %d, Key: %s, Name: %s, Defects: %d, Updated: %s",
                stat.getId(),
                stat.getProjectKey(),
                stat.getProjectName(),
                stat.getDefectCount(),
                stat.getUpdatedAt()
            ));
        });
    }
*/
}
