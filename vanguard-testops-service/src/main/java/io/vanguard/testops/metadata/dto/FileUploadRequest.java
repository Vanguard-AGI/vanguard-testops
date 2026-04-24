package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FileUploadRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{file_upload.project_id.not_blank}")
    @Size(min = 1, max = 50, message = "{file_upload.project_id.length_range}")
    private String projectId;

    @Schema(description = "存储类型: LOCAL/MINIO/OSS/S3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String storageType;

    @Schema(description = "分类: DATA(测试数据)/CERT(证书)/ATTACHMENT(附件)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String category;
}

