package io.vanguard.testops.bug.service;

import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.bug.domain.BugExample;
import io.vanguard.testops.bug.enums.BugPlatform;
import io.vanguard.testops.bug.mapper.BugMapper;
import io.vanguard.testops.plugin.platform.dto.SelectOption;
import io.vanguard.testops.plugin.platform.spi.Platform;
import io.vanguard.testops.project.service.ProjectApplicationService;
import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.BaseStatusFlowSettingService;
import jakarta.annotation.Resource;
import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.utils.SessionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BugStatusService {
    @Resource
    private BugMapper bugMapper;
    @Resource
    private ProjectApplicationService projectApplicationService;
    @Resource
    private BaseStatusFlowSettingService baseStatusFlowSettingService;
    @Resource
    private FeishuMeegoService feishuMeegoService;

    /**
     * 获取表头缺陷状态选项
     * @param projectId 项目ID
     * @return 选项集合
     */
    public List<SelectOption> getHeaderStatusOption(String projectId) {
        String platformName = projectApplicationService.getPlatformName(projectId);
        if (StringUtils.equals(platformName, BugPlatform.LOCAL.getName())) {
            // Local状态流
            return getAllLocalStatusOptions(projectId);
        } else if (StringUtils.equals(platformName, BugPlatform.FEISHU.name())) {
            UserDTO user = SessionUtils.getUser();
            if (user != null) {
                String projectKey = feishuMeegoService.getDefaultProjectKey();
                List<Map<String, String>> feishuOptions = feishuMeegoService.getWorkflowStatusOptions(projectKey, user.getEmail());
                if (CollectionUtils.isNotEmpty(feishuOptions)) {
                    return feishuOptions.stream().map(map -> {
                        SelectOption option = new SelectOption();
                        option.setText(map.get("text"));
                        option.setValue(map.get("value"));
                        return option;
                    }).toList();
                }
            }
            return new ArrayList<>();
        } else {
            // 第三方平台状态流
            Platform platform = projectApplicationService.getPlatform(projectId, true);
            String projectConfig = projectApplicationService.getProjectBugThirdPartConfig(projectId);
            String issueKey;
            if (StringUtils.equals(platformName, BugPlatform.JIRA.name())) {
                // 如果是Jira平台, 获取一条最新的缺陷默认Key作为参数
                issueKey = getJiraPlatformBugKeyLatest(projectId);
            } else {
                // 其余平台获取表头状态流暂不需要issue key参数
                issueKey = platformName;
            }
            List<SelectOption> platformStatusOption = new ArrayList<>();
            try {
                platformStatusOption = platform.getStatusTransitions(projectConfig, issueKey, null);
            } catch (Exception e) {
                LogUtils.error("获取平台状态选项有误: " + e.getMessage());
            }
            return platformStatusOption;
        }
    }

    /**
     * 获取缺陷下一批状态流转选项
     * @param projectId 项目ID
     * @param fromStatusId 当前状态选项值ID
     * @param platformBugKey 平台缺陷Key
     * @return 选项集合
     */
   public List<SelectOption> getToStatusItemOption(String projectId, String fromStatusId, String platformBugKey, Boolean showLocal) {
       String platformName = projectApplicationService.getPlatformName(projectId);
       // 若当前环境已配置 Feishu projectKey 且传入了 platformBugKey，则强制按 Feishu 状态流处理，
       // 避免项目平台仍为 LOCAL 时只能看到本地状态（例如只有“新建”）。
       String feishuProjectKey = feishuMeegoService.getDefaultProjectKey();
       boolean hasFeishuContext = StringUtils.isNotBlank(feishuProjectKey) && StringUtils.isNotBlank(platformBugKey);
       boolean isFeishuPlatform = StringUtils.equals(platformName, BugPlatform.FEISHU.name()) || hasFeishuContext;

       if (!isFeishuPlatform && (StringUtils.equals(platformName, BugPlatform.LOCAL.getName()) || BooleanUtils.isTrue(showLocal))) {
           // Local状态流
           return getToStatusItemOptionOnLocal(projectId, fromStatusId);
       } else if (isFeishuPlatform) {
           // Feishu 状态流：基于当前用户和对应 work_item_id 的飞书查询返回值获取有权限的状态选项
           UserDTO user = SessionUtils.getUser();
           if (user != null) {
               return feishuMeegoService.getValidStatusTransitions(feishuProjectKey, user.getEmail(), platformBugKey);
           }
           return new ArrayList<>();
       } else {
           // 第三方平台状态流
           // 获取配置平台, 获取第三方平台状态流
           Platform platform = projectApplicationService.getPlatform(projectId, true);
           String projectConfig = projectApplicationService.getProjectBugThirdPartConfig(projectId);
           List<SelectOption> platformOption = new ArrayList<>();
           try {
               platformOption =  platform.getStatusTransitions(projectConfig, platformBugKey, fromStatusId);
           } catch (Exception e) {
               LogUtils.error("获取平台状态选项有误: " + e.getMessage());
           }
           return platformOption;
       }
   }

   /**
    * 获取缺陷下一批状态流转选项
    * @param projectId 项目ID
    * @param fromStatusId 当前状态选项值ID
    * @return 选项集合
    */
   public List<SelectOption> getToStatusItemOptionOnLocal(String projectId, String fromStatusId) {
       return baseStatusFlowSettingService.getStatusTransitions(projectId, TemplateScene.BUG.name(), fromStatusId);
   }

    /**
     * 获取Local状态流选项
     * @param projectId 项目ID
     * @return 状态流选项
     */
   public List<SelectOption> getAllLocalStatusOptions(String projectId) {
       return baseStatusFlowSettingService.getAllStatusOption(projectId, TemplateScene.BUG.name());
   }

    /**
     * 获取当前项目最新的Jira平台缺陷Key (表头状态筛选需要)
     * @param projectId 项目ID
     * @return JiraKey
     */
   public String getJiraPlatformBugKeyLatest(String projectId) {
       BugExample example = new BugExample();
       example.createCriteria().andPlatformEqualTo(BugPlatform.JIRA.name()).andProjectIdEqualTo(projectId);
       example.setOrderByClause("create_time desc");
       List<Bug> bugs = bugMapper.selectByExample(example);
       if (CollectionUtils.isNotEmpty(bugs)) {
           return bugs.getFirst().getPlatformBugId();
       } else {
           return StringUtils.EMPTY;
       }
   }
}
