package io.vanguard.testops.project.dto.filemanagement;

import io.vanguard.testops.project.dto.filemanagement.request.FileBatchProcessRequest;
import io.vanguard.testops.project.dto.filemanagement.request.FileMetadataTableRequest;
import io.vanguard.testops.sdk.constants.StorageType;
import io.vanguard.testops.sdk.util.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class FileManagementQuery {
    public String projectId;
    public String keyword;
    public List<String> moduleIds;
    public String fileType;
    public String operator;
    public String storage = StorageType.OSS.name();
    public List<String> hiddenIds = new ArrayList<>();

    public FileManagementQuery(FileBatchProcessRequest batchProcessDTO) {
        this.projectId = batchProcessDTO.getProjectId();
        this.keyword = batchProcessDTO.getCondition().getKeyword();
        this.moduleIds = batchProcessDTO.getModuleIds();
        this.fileType = batchProcessDTO.getFileType();
        if (MapUtils.isNotEmpty(batchProcessDTO.getCondition().getCombine())) {
            if (batchProcessDTO.getCondition().getCombine().get("createUser") != null) {
                this.operator = batchProcessDTO.getCondition().getCombine().get("createUser").toString();
            }
            if (batchProcessDTO.getCondition().getCombine().get("storage") != null) {
                this.storage = batchProcessDTO.getCondition().getCombine().get("storage").toString();
            }
        }
    }

    public FileManagementQuery(FileMetadataTableRequest batchProcessDTO) {
        this.projectId = batchProcessDTO.getProjectId();
        this.keyword = batchProcessDTO.getKeyword();
        this.moduleIds = batchProcessDTO.getModuleIds();
        this.fileType = batchProcessDTO.getFileType();
        if (MapUtils.isNotEmpty(batchProcessDTO.getCombine())) {
            if (batchProcessDTO.getCombine().get("createUser") != null) {
                this.operator = batchProcessDTO.getCombine().get("createUser").toString();
            }
            if (batchProcessDTO.getCombine().get("storage") != null) {
                this.storage = batchProcessDTO.getCombine().get("storage").toString();
            }
            if (batchProcessDTO.getCombine().get("hiddenIds") != null) {
                this.hiddenIds = JSON.parseArray(
                        JSON.toJSONString(batchProcessDTO.getCombine().get("hiddenIds")));
            }
        }
    }
}
