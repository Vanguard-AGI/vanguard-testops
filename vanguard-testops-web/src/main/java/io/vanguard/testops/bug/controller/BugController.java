package io.vanguard.testops.bug.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.bug.constants.BugExportColumns;
import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.bug.dto.BugSyncResult;
import io.vanguard.testops.bug.dto.request.*;
import io.vanguard.testops.bug.dto.response.BugColumnsOptionDTO;
import io.vanguard.testops.bug.dto.response.BugDTO;
import io.vanguard.testops.bug.dto.response.BugDetailDTO;
import io.vanguard.testops.bug.service.BugLogService;
import io.vanguard.testops.bug.service.BugNoticeService;
import io.vanguard.testops.bug.service.BugService;
import io.vanguard.testops.bug.service.BugSyncService;
import io.vanguard.testops.bug.service.FeishuDefectMigrationService;
import io.vanguard.testops.project.dto.ProjectTemplateOptionDTO;
import io.vanguard.testops.project.service.ProjectApplicationService;
import io.vanguard.testops.project.service.ProjectTemplateService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.constants.TemplateScene;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.dto.sdk.TemplateCustomFieldDTO;
import io.vanguard.testops.system.dto.sdk.TemplateDTO;
import io.vanguard.testops.system.dto.sdk.request.PosRequest;
import io.vanguard.testops.system.file.annotation.FileLimit;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.notice.annotation.SendNotice;
import io.vanguard.testops.system.notice.constants.NoticeConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jan
 */
@Tag(name = "缺陷管理")
@RestController
@RequestMapping("/bug")
public class BugController {

    @Resource
    private BugService bugService;
    @Resource
    private BugSyncService bugSyncService;
    @Resource
    private ProjectTemplateService projectTemplateService;
    @Resource
    private ProjectApplicationService projectApplicationService;
    @Resource
    private FeishuDefectMigrationService feishuDefectMigrationService;

    @GetMapping("/current-platform/{projectId}")
    @Operation(summary = "缺陷管理-列表-获取当前项目所属平台")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public String getCurrentPlatform(@PathVariable String projectId) {
        return projectApplicationService.getPlatformName(projectId);
    }

    @GetMapping("/header/custom-field/{projectId}")
    @Operation(summary = "缺陷管理-列表-获取表头自定义字段集合")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public List<TemplateCustomFieldDTO> getHeaderFields(@PathVariable String projectId) {
        return bugService.getHeaderCustomFields(projectId);
    }

    @GetMapping("/header/columns-option/{projectId}")
    @Operation(summary = "缺陷管理-列表-获取表头状态选项")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public BugColumnsOptionDTO getHeaderOption(@PathVariable String projectId) {
        return bugService.getHeaderOption(projectId);
    }

    @PostMapping("/page")
    @Operation(summary = "缺陷管理-列表-分页获取缺陷列表")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public Pager<List<BugDTO>> page(@Validated @RequestBody BugPageRequest request) {
        request.setUseTrash(false);
        if (request.getBoardCount() || request.getRelatedToPlan() || request.getCreateByMe() || request.getAssignedToMe() || request.getUnresolved()) {
            request.setTodoParam(bugService.buildBugToDoParam(request, SessionUtils.getUserId(), SessionUtils.getCurrentOrganizationId()));
        }
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "pos desc");
        return PageUtils.setPageInfo(page, bugService.list(request));
    }

    @FileLimit
    @PostMapping("/add")
    @Operation(summary = "缺陷管理-列表-创建缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_ADD)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog(#request, #files)", msClass = BugLogService.class)
    @SendNotice(taskType = NoticeConstants.TaskType.BUG_TASK, event = NoticeConstants.Event.CREATE, target = "#targetClass.getNoticeByRequest(#request)", targetClass = BugNoticeService.class)
    public Bug add(@Validated({Created.class}) @RequestPart(value = "request") BugEditRequest request,
                   @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return bugService.addOrUpdate(request, files, SessionUtils.getUserId(), SessionUtils.getCurrentOrganizationId(), false);
    }

    @FileLimit
    @PostMapping("/update")
    @Operation(summary = "缺陷管理-列表-编辑缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request, #files)", msClass = BugLogService.class)
    @SendNotice(taskType = NoticeConstants.TaskType.BUG_TASK, event = NoticeConstants.Event.UPDATE, target = "#targetClass.getNoticeByRequest(#request)", targetClass = BugNoticeService.class)
    public Bug update(@Validated({Updated.class}) @RequestPart(value = "request") BugEditRequest request,
                       @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return bugService.addOrUpdate(request, files, SessionUtils.getUserId(), SessionUtils.getCurrentOrganizationId(), true);
    }

    @GetMapping("/check-exist/{id}")
    @Operation(summary = "缺陷管理-列表-校验缺陷是否存在")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public boolean check(@PathVariable String id) {
        return bugService.checkExist(id);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "缺陷管理-列表-查看缺陷(详情&&编辑&&复制)")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public BugDetailDTO get(@PathVariable String id) {
        return bugService.get(id, SessionUtils.getUserId(), Objects.requireNonNull(SessionUtils.getUser()).getLanguage());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "缺陷管理-列表-删除缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#id)", msClass = BugLogService.class)
    @SendNotice(taskType = NoticeConstants.TaskType.BUG_TASK, event = NoticeConstants.Event.DELETE, target = "#targetClass.getNoticeById(#id)", targetClass = BugNoticeService.class)
    public void delete(@PathVariable String id,
                       @RequestParam(required = false, defaultValue = "false") Boolean deleteFeishu) {
        bugService.delete(id, SessionUtils.getUserId(), Boolean.TRUE.equals(deleteFeishu));
    }

    @GetMapping("/feishu-raw/{id}")
    @Operation(summary = "调试-飞书缺陷详情与评论原始结构（用于确认 multi_texts、评论 doc_rich_text 等）")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public Map<String, Object> getFeishuRaw(@PathVariable String id) {
        return bugService.getFeishuRawDetailAndComments(id);
    }

    @GetMapping("/feishu-image")
    @Operation(summary = "飞书缺陷详情中的图片代理（根据 UUID 从飞书下载后返回）")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public ResponseEntity<byte[]> getFeishuImage(@RequestParam String bugId, @RequestParam String uuid) {
        return bugService.getFeishuImage(bugId, uuid);
    }

    @GetMapping("/feishu-comments/{id}")
    @Operation(summary = "飞书缺陷-实时评论列表（直接从飞书拉取，用于详情页展示）")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public List<Map<String, Object>> getFeishuComments(@PathVariable String id) {
        return bugService.getFeishuComments(id);
    }

    @GetMapping("/sync/{projectId}")
    @Operation(summary = "缺陷管理-列表-同步缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public void sync(@PathVariable String projectId) {
        bugSyncService.syncBugs(projectId, SessionUtils.getUserId(), Objects.requireNonNull(SessionUtils.getUser()).getLanguage(), Translator.get("sync_mode.manual"));
    }

    @PostMapping("/sync/all")
    @Operation(summary = "缺陷管理-列表-同步缺陷(区间)")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public void syncAll(@RequestBody BugSyncRequest request) {
        bugSyncService.syncAllBugs(request, SessionUtils.getUserId(), Objects.requireNonNull(SessionUtils.getUser()).getLanguage(), Translator.get("sync_mode.manual"));
    }

    @GetMapping("/sync/check/{projectId}")
    @Operation(summary = "缺陷管理-列表-校验缺陷同步状态")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public BugSyncResult checkStatus(@PathVariable String projectId) {
        return bugSyncService.checkSyncStatus(projectId);
    }

    @GetMapping("/export/columns/{projectId}")
    @Operation(summary = "缺陷管理-列表-获取导出字段配置")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public BugExportColumns getExportColumns(@PathVariable String projectId) {
        return bugService.getExportColumns(projectId);
    }

    @PostMapping("/export")
    @Operation(summary = "缺陷管理-列表-批量导出缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_EXPORT)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public ResponseEntity<byte[]> export(@Validated @RequestBody BugExportRequest request) throws Exception {
        request.setSort(StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "pos desc");
        request.setUseTrash(false);
        return bugService.export(request);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "缺陷管理-列表-批量删除缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_DELETE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.batchDeleteLog(#request)", msClass = BugLogService.class)
    @SendNotice(taskType = NoticeConstants.TaskType.BUG_TASK, event = NoticeConstants.Event.DELETE, target = "#targetClass.getBatchNoticeByRequest(#request)", targetClass = BugNoticeService.class)
    public void batchDelete(@Validated @RequestBody BugBatchRequest request) {
        request.setUseTrash(false);
        bugService.batchDelete(request, SessionUtils.getUserId());
    }

    @PostMapping("/batch-update")
    @Operation(summary = "缺陷管理-列表-批量编辑缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    @SendNotice(taskType = NoticeConstants.TaskType.BUG_TASK, event = NoticeConstants.Event.UPDATE, target = "#targetClass.getBatchNoticeByRequest(#request)", targetClass = BugNoticeService.class)
    public void batchUpdate(@Validated @RequestBody BugBatchUpdateRequest request) {
        request.setUseTrash(false);
        bugService.batchUpdate(request, SessionUtils.getUserId());
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "缺陷管理-列表-拖拽排序")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_UPDATE)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public void editPos(@Validated @RequestBody PosRequest request) {
        bugService.editPos(request);
    }

    @GetMapping("/template/option/{projectId}")
    @Operation(summary = "缺陷管理-详情-获取当前项目模板选项")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#projectId", resourceType = "project")
    public List<ProjectTemplateOptionDTO> getTemplateOption(@PathVariable String projectId) {
        return projectTemplateService.getOption(projectId, TemplateScene.BUG.name());
    }

    @PostMapping("/template/detail")
    @Operation(summary = "缺陷管理-详情-获取模板详情内容")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public TemplateDTO getTemplateDetail(@RequestBody BugTemplateRequest request) {
        return bugService.getTemplate(request.getId(), request.getProjectId(), request.getFromStatusId(), request.getPlatformBugKey(), request.getShowLocal(), Objects.requireNonNull(SessionUtils.getUser()).getLanguage());
    }

    @GetMapping("/follow/{id}")
    @Operation(summary = "缺陷管理-详情-关注缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public void follow(@PathVariable String id) {
        bugService.follow(id, SessionUtils.getUserId());
    }

    @GetMapping("/unfollow/{id}")
    @Operation(summary = "缺陷管理-详情-取消关注缺陷")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public void unfollow(@PathVariable String id) {
        bugService.unfollow(id, SessionUtils.getUserId());
    }

    @GetMapping("/feishu/business-line-options")
    @Operation(summary = "飞书业务线选项（写死映射表，供前端下拉），value=id、展示 name，带 level/path 层级")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public List<Map<String, String>> getFeishuBusinessLineOptions(@RequestParam(required = false) String projectKey) {
        return io.vanguard.testops.bug.constants.FeishuOptionMapping.getBusinessLineOptionsStatic();
    }

    @GetMapping("/feishu/defect-reason-options")
    @Operation(summary = "飞书缺陷原因选项（从飞书 Open API 拉取 field_e84b00 枚举，供前端下拉），与业务线一样不走写死")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public List<Map<String, Object>> getFeishuDefectReasonOptions(
            @RequestParam(required = false) String projectKey) {
        String userEmail = SessionUtils.getUser() != null && SessionUtils.getUser().getEmail() != null
                ? SessionUtils.getUser().getEmail() : null;
        return bugService.getFeishuDefectReasonOptions(projectKey, userEmail);
    }

    @GetMapping("/feishu/field-options")
    @Operation(summary = "飞书缺陷字段枚举（通用，按 fieldKey 从 Open API 拉取），供前端全部枚举下拉不走写死。fieldKey 如 priority、business、field_1cbc4e、field_39dbe4、field_0b1b4f、field_6b822e、field_f12022")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public List<Map<String, Object>> getFeishuFieldOptions(
            @RequestParam String fieldKey,
            @RequestParam(required = false) String projectKey) {
        String userEmail = SessionUtils.getUser() != null && SessionUtils.getUser().getEmail() != null
                ? SessionUtils.getUser().getEmail() : null;
        return bugService.getFeishuFieldOptions(projectKey, userEmail, fieldKey);
    }

    @GetMapping("/feishu/history/{id}")
    @Operation(summary = "飞书缺陷-变更历史（从飞书操作记录接口拉取）")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    public List<Map<String, Object>> getFeishuHistory(@PathVariable String id) {
        LogUtils.info("[Feishu] getFeishuHistory 接口被调用, bugId={}", id);
        return bugService.getFeishuChangeHistory(id);
    }

    @PostMapping("/feishu/migrate")
    @Operation(summary = "缺陷管理-飞书缺陷一次性迁移")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_ADD)
    public ResponseEntity<String> migrateFeishuDefects(@RequestBody java.util.Map<String, String> params) {
        String projectKey = params.getOrDefault("projectKey", "");
        String userEmail = params.getOrDefault("userEmail", "");
        int count = feishuDefectMigrationService.migrateAll(projectKey, userEmail);
        return ResponseEntity.ok("迁移完成，新增 " + count + " 条缺陷");
    }
}
