package io.vanguard.testops.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.vanguard.testops.handler.DateTimeTypeHandler;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName(value = "metadata_file_resource", schema = "spotter_aegis")
public class MetadataFileResource implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.file_id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 64, message = "{metadata_file_resource.file_id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @TableField("project_id")
    @Schema(description = "归属项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.project_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{metadata_file_resource.project_id.length_range}", groups = {Created.class, Updated.class})
    private String projectId;

    @TableField("storage_name")
    @Schema(description = "存储文件名/Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.storage_name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{metadata_file_resource.storage_name.length_range}", groups = {Created.class, Updated.class})
    private String storageName;

    @TableField("storage_type")
    @Schema(description = "存储方式: LOCAL/MINIO/S3/OSS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.storage_type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{metadata_file_resource.storage_type.length_range}", groups = {Created.class, Updated.class})
    private String storageType;

    @Schema(description = "存放路径或URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.path.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 500, message = "{metadata_file_resource.path.length_range}", groups = {Created.class, Updated.class})
    private String path;

    @TableField("file_size")
    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_file_resource.file_size.not_blank}", groups = {Created.class})
    private Long fileSize;

    @Schema(description = "文件后缀")
    @Size(max = 20, message = "{metadata_file_resource.extension.length_range}", groups = {Created.class, Updated.class})
    private String extension;

    @TableField("content_type")
    @Schema(description = "MIME类型")
    @Size(max = 100, message = "{metadata_file_resource.content_type.length_range}", groups = {Created.class, Updated.class})
    private String contentType;

    @Schema(description = "文件MD5/SHA256, 用于防止重复上传")
    @Size(max = 64, message = "{metadata_file_resource.checksum.length_range}", groups = {Created.class, Updated.class})
    private String checksum;

    @Schema(description = "分类: DATA(测试数据)/CERT(证书)/ATTACHMENT(附件)")
    @Size(max = 32, message = "{metadata_file_resource.category.length_range}", groups = {Created.class, Updated.class})
    private String category;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_file_resource.create_user.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{metadata_file_resource.create_user.length_range}", groups = {Created.class, Updated.class})
    private String createUser;

    @TableField("create_time")
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_file_resource.create_time.not_blank}", groups = {Created.class})
    private Long createTime;

    @TableField(value = "deleted_time", typeHandler = DateTimeTypeHandler.class)
    @Schema(description = "删除时间")
    private Long deletedTime;
}

