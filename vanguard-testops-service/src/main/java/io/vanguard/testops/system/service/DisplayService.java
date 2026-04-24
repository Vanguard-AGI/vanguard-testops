package io.vanguard.testops.system.service;

import io.vanguard.testops.sdk.constants.DefaultRepositoryDir;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.file.FileRequest;
import io.vanguard.testops.sdk.file.OssRepository;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.domain.SystemParameter;
import io.vanguard.testops.system.dto.DisplayConfigDTO;
import io.vanguard.testops.system.dto.PageConfigRequest;
import io.vanguard.testops.system.dto.PageConfigResponse;
import io.vanguard.testops.system.mapper.SystemParameterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class DisplayService {

    @Resource
    private SystemParameterMapper systemParameterMapper;

    @Resource
    private OssRepository minioRepository;

    public void saveDisplayConfig(MultipartFile requestFile, List<MultipartFile> files) {
        try {
            String requestStr = new String(requestFile.getBytes());
            List<PageConfigRequest> requests = JSON.parseArray(requestStr, PageConfigRequest.class);
            
            for (PageConfigRequest request : requests) {
                if ("file".equals(request.getType())) {
                    handleFileUpload(request, files);
                } else if ("text".equals(request.getType())) {
                    saveOrUpdateParameter(request.getParamKey(), request.getParamValue());
                }
            }
        } catch (Exception e) {
            throw new MSException("保存页面配置失败: " + e.getMessage());
        }
    }

    private void handleFileUpload(PageConfigRequest request, List<MultipartFile> files) {
        try {
            // 如果是原始值（删除文件），直接删除参数
            if (request.getOriginal() != null && request.getOriginal()) {
                deleteParameter(request.getParamKey());
                return;
            }

            // 如果有文件上传
            if (request.getHasFile() != null && request.getHasFile() && files != null) {
                // 从文件列表中找到对应的文件
                String filePrefix = request.getParamKey() + ",";
                MultipartFile targetFile = files.stream()
                    .filter(file -> file.getOriginalFilename() != null && 
                           file.getOriginalFilename().startsWith(filePrefix))
                    .findFirst()
                    .orElse(null);

                if (targetFile != null) {
                    // 上传文件到 OSS
                    FileRequest fileRequest = new FileRequest();
                    fileRequest.setFileName(request.getFileName());
                    fileRequest.setFolder(DefaultRepositoryDir.getSystemRootDir());
                    minioRepository.saveFile(targetFile, fileRequest);

                    // 保存文件名到系统参数
                    saveOrUpdateParameter(request.getParamKey(), request.getFileName());
                }
            }
        } catch (Exception e) {
            throw new MSException("文件上传失败: " + e.getMessage());
        }
    }

    public void saveDisplayConfig(DisplayConfigDTO displayConfig) {
        // 保存主题颜色
        if (displayConfig.getThemeColor() != null) {
            saveOrUpdateParameter("ui.theme.color", displayConfig.getThemeColor());
        }

        // 保存系统名称
        if (displayConfig.getSystemName() != null) {
            saveOrUpdateParameter("ui.system.name", displayConfig.getSystemName());
        }

        // 保存系统描述
        if (displayConfig.getSystemDescription() != null) {
            saveOrUpdateParameter("ui.system.description", displayConfig.getSystemDescription());
        }

        // 保存自定义主题开关
        if (displayConfig.getEnableCustomTheme() != null) {
            saveOrUpdateParameter("ui.theme.enable", displayConfig.getEnableCustomTheme().toString());
        }

        // 保存自定义Logo开关
        if (displayConfig.getEnableCustomLogo() != null) {
            saveOrUpdateParameter("ui.logo.enable", displayConfig.getEnableCustomLogo().toString());
        }
    }

    public List<PageConfigResponse> getDisplayConfigList() {
        List<PageConfigResponse> configs = new ArrayList<>();
        
        // 文件类型参数
        addFileConfig(configs, "ui.icon");
        addFileConfig(configs, "ui.loginLogo");
        addFileConfig(configs, "ui.loginImage");
        addFileConfig(configs, "ui.logoPlatform");
        
        // 文本类型参数
        addTextConfig(configs, "ui.slogan");
        addTextConfig(configs, "ui.title");
        addTextConfig(configs, "ui.style");
        addTextConfig(configs, "ui.theme");
        addTextConfig(configs, "ui.helpDoc");
        addTextConfig(configs, "ui.platformName");
        
        return configs;
    }
    
    private void addFileConfig(List<PageConfigResponse> configs, String paramKey) {
        String paramValue = getParameterValue(paramKey);
        if (paramValue != null) {
            PageConfigResponse config = new PageConfigResponse();
            config.setParamKey(paramKey);
            config.setParamValue(paramValue);
            config.setType("file");
            config.setFile(paramValue);
            config.setFileName(paramValue);
            configs.add(config);
        }
    }
    
    private void addTextConfig(List<PageConfigResponse> configs, String paramKey) {
        String paramValue = getParameterValue(paramKey);
        if (paramValue != null) {
            PageConfigResponse config = new PageConfigResponse();
            config.setParamKey(paramKey);
            config.setParamValue(paramValue);
            config.setType("text");
            configs.add(config);
        } else if ("ui.helpDoc".equals(paramKey)) {
            PageConfigResponse config = new PageConfigResponse();
            config.setParamKey(paramKey);
            config.setParamValue("https://spotterio.feishu.cn/wiki/F5A3wNjHziasXokWPRZclZKnhi?fromScene=spaceOverview");
            config.setType("text");
            configs.add(config);
        }
    }

    public DisplayConfigDTO getDisplayConfig() {
        DisplayConfigDTO config = new DisplayConfigDTO();
        
        // 获取主题颜色
        config.setThemeColor(getParameterValue("ui.theme.color"));
        // 获取系统名称
        config.setSystemName(getParameterValue("ui.system.name"));
        // 获取系统描述
        config.setSystemDescription(getParameterValue("ui.system.description"));
        // 获取自定义主题开关
        String themeEnable = getParameterValue("ui.theme.enable");
        if (themeEnable != null) {
            config.setEnableCustomTheme(Boolean.valueOf(themeEnable));
        }
        // 获取自定义Logo开关
        String logoEnable = getParameterValue("ui.logo.enable");
        if (logoEnable != null) {
            config.setEnableCustomLogo(Boolean.valueOf(logoEnable));
        }

        return config;
    }

    private void saveOrUpdateParameter(String key, String value) {
        SystemParameter param = systemParameterMapper.selectByPrimaryKey(key);
        if (param == null) {
            param = new SystemParameter();
            param.setParamKey(key);
            param.setParamValue(value);
            param.setType("text");
            systemParameterMapper.insert(param);
        } else {
            param.setParamValue(value);
            systemParameterMapper.updateByPrimaryKeySelective(param);
        }
    }

    private String getParameterValue(String key) {
        SystemParameter param = systemParameterMapper.selectByPrimaryKey(key);
        return param != null ? param.getParamValue() : null;
    }

    private void deleteParameter(String key) {
        systemParameterMapper.deleteByPrimaryKey(key);
    }
}
