package io.vanguard.testops.project.controller;


import io.vanguard.testops.project.dto.MessageTemplateResultDTO;
import io.vanguard.testops.project.service.NoticeTemplateService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;


@Tag(name = "项目管理-消息设置-模版设置")
@RestController
@RequestMapping("notice/template/")
public class NoticeTemplateController {
    @Resource
    private NoticeTemplateService noticeTemplateService;

    @GetMapping("get/fields/{projectId}")
    @Operation(summary = "项目管理-消息设置-模版设置-获取消息模版字段")
    @RequiresPermissions(PermissionConstants.PROJECT_MESSAGE_READ)
    public MessageTemplateResultDTO getTemplateFields(@PathVariable String projectId, @Schema(description = "消息配置功能类型")
    @RequestParam(value = "taskType") String taskType) {
        return noticeTemplateService.getTemplateFields(projectId, taskType);
    }

}

