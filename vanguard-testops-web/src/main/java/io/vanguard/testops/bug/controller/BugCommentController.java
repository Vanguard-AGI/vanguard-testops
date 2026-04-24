package io.vanguard.testops.bug.controller;

import io.vanguard.testops.bug.domain.BugComment;
import io.vanguard.testops.bug.dto.request.BugCommentEditRequest;
import io.vanguard.testops.bug.dto.response.BugCommentDTO;
import io.vanguard.testops.bug.service.BugCommentService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "缺陷管理-评论")
@RestController
@RequestMapping("/bug/comment")
public class BugCommentController {

    @Resource
    private BugCommentService bugCommentService;

    @GetMapping("/get/{bugId}")
    @Operation(summary = "缺陷管理-评论-获取评论集合")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_READ)
    @CheckOwner(resourceId = "#bugId", resourceType = "bug")
    public List<BugCommentDTO> get(@PathVariable String bugId) {
        return bugCommentService.getComments(bugId);
    }

    @PostMapping("/add")
    @Operation(summary = "缺陷管理-评论-新增/回复评论")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_COMMENT)
    @CheckOwner(resourceId = "#request.getBugId()", resourceType = "bug")
    public BugComment add(@RequestBody BugCommentEditRequest request) {
        return bugCommentService.addComment(request, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "缺陷管理-评论-编辑评论")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_COMMENT)
    @CheckOwner(resourceId = "#request.getBugId()", resourceType = "bug")
    public BugComment update(@RequestBody BugCommentEditRequest request) {
        return bugCommentService.updateComment(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{commentId}")
    @Operation(summary = "缺陷管理-评论-删除评论")
    @RequiresPermissions(PermissionConstants.PROJECT_BUG_COMMENT)
    public void delete(@PathVariable String commentId) {
        bugCommentService.deleteComment(commentId, SessionUtils.getUserId());
    }
}
