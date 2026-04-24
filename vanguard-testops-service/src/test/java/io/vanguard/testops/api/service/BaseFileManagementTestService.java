package io.vanguard.testops.api.service;

import io.vanguard.testops.project.domain.FileAssociation;
import io.vanguard.testops.project.domain.FileAssociationExample;
import io.vanguard.testops.project.dto.filemanagement.FileLogRecord;
import io.vanguard.testops.project.dto.filemanagement.request.FileBatchProcessRequest;
import io.vanguard.testops.project.dto.filemanagement.request.FileReUploadRequest;
import io.vanguard.testops.project.dto.filemanagement.request.FileUploadRequest;
import io.vanguard.testops.project.mapper.FileAssociationMapper;
import io.vanguard.testops.project.service.FileAssociationService;
import io.vanguard.testops.project.service.FileManagementService;
import io.vanguard.testops.project.service.FileMetadataService;
import io.vanguard.testops.system.base.BaseTest;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.utils.SessionUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-20  11:32
 */
@Service
public class BaseFileManagementTestService {

    @Resource
    private FileMetadataService fileMetadataService;
    @Resource
    private FileAssociationService fileAssociationService;
    @Resource
    private FileAssociationMapper fileAssociationMapper;
    @Resource
    private FileManagementService fileManagementService;

    public String upload(MultipartFile uploadFile) throws Exception {
        FileUploadRequest request = new FileUploadRequest();
        request.setProjectId(BaseTest.DEFAULT_PROJECT_ID);
        request.setEnable(false);
        return fileMetadataService.upload(request, "admin", uploadFile);
    }

    public String reUpload(String fileId, MultipartFile uploadFile) throws Exception {
        FileReUploadRequest request = new FileReUploadRequest();
        request.setFileId(fileId);
        return fileMetadataService.reUpload(request, "admin", uploadFile);
    }

    public String upgrade(String originFileId, String resourceId) {
        FileLogRecord fileLogRecord = FileLogRecord.builder()
                .logModule(OperationLogModule.PROJECT_FILE_MANAGEMENT)
                .operator(SessionUtils.getUserId())
                .projectId(BaseTest.DEFAULT_PROJECT_ID)
                .build();
        FileAssociationExample example = new FileAssociationExample();
        example.createCriteria().andFileIdEqualTo(originFileId)
                        .andSourceIdEqualTo(resourceId);
        FileAssociation fileAssociation = fileAssociationMapper.selectByExample(example).getFirst();
        return fileAssociationService.upgrade(fileAssociation.getId(), fileLogRecord);
    }

    public void deleteFile(String fileId) {
        FileBatchProcessRequest request = new FileBatchProcessRequest();
        request.setSelectIds(List.of(fileId));
        request.setProjectId(BaseTest.DEFAULT_PROJECT_ID);
        fileManagementService.delete(request, SessionUtils.getUserId());
    }
}
