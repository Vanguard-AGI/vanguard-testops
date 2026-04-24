package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.FileMetadata;
import io.vanguard.testops.project.domain.FileMetadataRepository;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.builder.LogDTOBuilder;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class FileMetadataLogService {

    private String logModule = OperationLogModule.PROJECT_FILE_MANAGEMENT;

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private OperationLogService operationLogService;

    public void saveUploadLog(FileMetadata module, String operator) {
        Project project = projectMapper.selectByPrimaryKey(module.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(module.getProjectId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.ADD.name())
                .module(logModule)
                .method(HttpMethodConstants.POST.name())
                .path("/project/file/upload")
                .sourceId(module.getId())
                .content(Translator.get("file.log.upload") + " " + module.getName())
                .originalValue(JSON.toJSONBytes(module))
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void saveReUploadLog(FileMetadata module, String operator) {
        Project project = projectMapper.selectByPrimaryKey(module.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(module.getProjectId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.UPDATE.name())
                .module(logModule)
                .method(HttpMethodConstants.POST.name())
                .path("/project/file/re-upload")
                .sourceId(module.getId())
                .content(Translator.get("file.log.re-upload") + " " + module.getName())
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void saveUpdateLog(FileMetadata oldFile, FileMetadata newFile, String projectId, String operator) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(projectId)
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.UPDATE.name())
                .module(logModule)
                .method(HttpMethodConstants.POST.name())
                .path("/project/file/update")
                .sourceId(newFile.getId())
                .content(newFile.getName())
                .originalValue(JSON.toJSONBytes(oldFile))
                .modifiedValue(JSON.toJSONBytes(newFile))
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void saveDeleteLog(List<FileMetadata> deleteList, String projectId, String operator) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<LogDTO> list = new ArrayList<>();
        for (FileMetadata fileMetadata : deleteList) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(projectId)
                    .organizationId(project.getOrganizationId())
                    .type(OperationLogType.DELETE.name())
                    .module(logModule)
                    .method(HttpMethodConstants.POST.name())
                    .path("/project/file/delete")
                    .sourceId(fileMetadata.getId())
                    .content(fileMetadata.getName())
                    .originalValue(JSON.toJSONBytes(fileMetadata))
                    .createUser(operator)
                    .build().getLogDTO();
            list.add(dto);
        }

        operationLogService.batchAdd(list);
    }

    public void saveChangeJarFileStatusLog(FileMetadata module, boolean enable, String operator) {
        Project project = projectMapper.selectByPrimaryKey(module.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(project.getId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.UPDATE.name())
                .module(logModule)
                .method(HttpMethodConstants.GET.name())
                .path("/project/file/jar-file-status")
                .sourceId(module.getId())
                .content(Translator.get("change.jar.enable") + ":" + enable)
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void saveFileMoveLog(List<FileMetadata> logList, String projectId, String operator) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<LogDTO> list = new ArrayList<>();
        for (FileMetadata fileMetadata : logList) {
            LogDTO dto = LogDTOBuilder.builder()
                    .projectId(projectId)
                    .organizationId(project.getOrganizationId())
                    .type(OperationLogType.UPDATE.name())
                    .module(logModule)
                    .method(HttpMethodConstants.POST.name())
                    .path("/project/file/batch-move")
                    .sourceId(fileMetadata.getId())
                    .content(Translator.get("file.log.change_file_module") + ":" + fileMetadata.getName())
                    .createUser(operator)
                    .build().getLogDTO();
            list.add(dto);
        }

        operationLogService.batchAdd(list);
    }

    public void saveRepositoryAddLog(FileMetadata fileMetadata, FileMetadataRepository repositoryFile, String operator) {
        Project project = projectMapper.selectByPrimaryKey(fileMetadata.getProjectId());
        Map<String, Object> logContent = new HashMap<>();
        logContent.put("fileMetadata", fileMetadata);
        logContent.put("repositoryFile", repositoryFile);
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(fileMetadata.getProjectId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.ADD.name())
                .module(logModule)
                .method(HttpMethodConstants.POST.name())
                .path("/project/file/repository/add-file")
                .sourceId(fileMetadata.getId())
                .content(Translator.get("file.log.repository.add") + " " + fileMetadata.getName())
                .originalValue(JSON.toJSONBytes(logContent))
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void saveFilePullLog(FileMetadata oldFile, FileMetadata module, String operator) {
        Project project = projectMapper.selectByPrimaryKey(module.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(module.getProjectId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.UPDATE.name())
                .module(logModule)
                .method(HttpMethodConstants.GET.name())
                .path("/project/file/repository/pull-file")
                .sourceId(module.getId())
                .content(Translator.get("file.log.pull") + " " + module.getName())
                .originalValue(JSON.toJSONBytes(oldFile))
                .modifiedValue(JSON.toJSONBytes(module))
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }
}
