package io.vanguard.testops.metadata.controller;

import io.vanguard.testops.metadata.domain.MetadataDefinition;
import io.vanguard.testops.metadata.domain.MetadataFileResource;
import io.vanguard.testops.metadata.dto.*;
import io.vanguard.testops.metadata.service.MetadataDefinitionService;
import io.vanguard.testops.metadata.service.MetadataFileResourceService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据定义控制器
 */
@RestController
@RequestMapping("/metadata/definition")
@Tag(name = "元数据管理-元数据定义")
public class MetadataDefinitionController {

    @Resource
    private MetadataDefinitionService metadataDefinitionService;

    @Resource
    private MetadataFileResourceService metadataFileResourceService;

    @PostMapping("/add")
    @Operation(summary = "创建元数据定义")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    // @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public String add(@Validated @RequestBody MetadataDefinitionAddRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法创建元数据定义");
        }
        return metadataDefinitionService.create(request, userId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新元数据定义")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "")
    public String update(@Validated @RequestBody MetadataDefinitionUpdateRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法更新元数据定义");
        }
        return metadataDefinitionService.update(request, userId);
    }

    @PostMapping("/batch/move/module")
    @Operation(summary = "批量移动模块ID")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "")
    public void batchMoveModule(@Validated @RequestBody MetadataDefinitionBatchMoveModuleRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法批量移动模块");
        }
        metadataDefinitionService.batchMoveModule(request, userId);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除元数据定义")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "")
    public void delete(@PathVariable String id) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法删除元数据定义");
        }
        metadataDefinitionService.delete(id, userId);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取元数据定义详情")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    public MetadataDefinitionDTO get(@PathVariable String id) {
        return metadataDefinitionService.get(id);
    }

    @GetMapping("/script/{scriptId}")
    @Operation(summary = "根据 script_id 获取脚本详情")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    public io.vanguard.testops.metadata.domain.ScriptManage getScript(@PathVariable String scriptId) {
        return metadataDefinitionService.getScriptById(scriptId);
    }

    @PostMapping("/script/run")
    @Operation(summary = "执行脚本：根据 definition_id 组装 script、request_config 参数后调用 Aegis test-async，轮询结果后返回统一格式")
    public ScriptRunResponse scriptRun(@Validated @RequestBody ScriptRunRequest request) {
        return metadataDefinitionService.runScript(request);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询元数据定义列表")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    // @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public List<MetadataDefinition> page(@Validated @RequestBody MetadataDefinitionPageRequest request) {
        return metadataDefinitionService.list(request);
    }

    @PostMapping("/copy/{id}")
    @Operation(summary = "复制元数据定义")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    public String copy(@PathVariable String id) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法复制元数据定义");
        }
        return metadataDefinitionService.copy(id, userId);
    }

    @PostMapping("/file/upload")
    @Operation(summary = "上传文件（支持查询参数方式：?id=projectId）")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("id") String projectId,
                             @RequestParam(value = "storageType", required = false) String storageType,
                             @RequestParam(value = "category", required = false) String category) throws Exception {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        
        return metadataFileResourceService.upload(file, projectId, storageType, category, userId);
    }

    @GetMapping("/file/download/{fileResourceId}")
    @Operation(summary = "下载文件")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileResourceId) throws Exception {
        byte[] fileBytes = metadataFileResourceService.download(fileResourceId);
        MetadataFileResource fileResource = metadataFileResourceService.get(fileResourceId);
        
        String fileName = fileResource.getStorageName();
        if (fileResource.getExtension() != null && !fileName.endsWith(fileResource.getExtension())) {
            fileName = fileName + fileResource.getExtension();
        }
        
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedFileName);
        headers.setContentLength(fileBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }

    @PostMapping("/import/swagger")
    @Operation(summary = "导入 Swagger API（异步执行）")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    public Map<String, Object> importSwagger(@Validated @RequestBody SwaggerImportRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            throw new MSException("用户未登录，无法导入 Swagger API");
        }
        metadataDefinitionService.importSwagger(request, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "导入任务已提交，正在后台异步执行");
        response.put("status", "success");
        return response;
    }

    @PostMapping("/import/dubbo/swagger")
    @Operation(summary = "导入 Dubbo Swagger API（异步执行）")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    public Map<String, Object> importDubboSwagger(@Validated @RequestBody DubboSwaggerImportRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        metadataDefinitionService.importDubboSwagger(request, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "导入任务已提交，正在后台异步执行");
        response.put("status", "success");
        return response;
    }

    @PostMapping("/import/ddl")
    @Operation(summary = "导入数据库表DDL（异步执行）")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    public Map<String, Object> importDatabaseTable(@Validated @RequestBody DatabaseTableImportRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        metadataDefinitionService.importDatabaseTable(request, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "导入任务已提交，正在后台异步执行");
        response.put("status", "success");
        return response;
    }
}
