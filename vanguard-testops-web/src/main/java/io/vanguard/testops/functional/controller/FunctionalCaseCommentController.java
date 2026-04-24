package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.functional.dto.FunctionalCaseCommentDTO;
import io.vanguard.testops.functional.request.FunctionalCaseCommentRequest;
import io.vanguard.testops.functional.service.FunctionalCaseCommentService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用例管理-功能用例-用例评论")
@RestController
@RequestMapping("/functional/case/comment")
public class FunctionalCaseCommentController {

    @Resource
    private FunctionalCaseCommentService functionalCaseCommentService;

    @PostMapping("/save")
    @Operation(summary = "用例管理-功能用例-用例评论-创建评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_COMMENT)
    @CheckOwner(resourceId = "#functionalCaseCommentRequest.getCaseId()", resourceType = "functional_case")
    public FunctionalCaseComment saveComment(@Validated({Created.class}) @RequestBody FunctionalCaseCommentRequest functionalCaseCommentRequest) {
        return functionalCaseCommentService.saveComment(functionalCaseCommentRequest, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "用例管理-功能用例-用例评论-修改评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_COMMENT)
    @CheckOwner(resourceId = "#functionalCaseCommentRequest.getCaseId()", resourceType = "functional_case")
    public FunctionalCaseComment updateComment(@Validated({Updated.class}) @RequestBody FunctionalCaseCommentRequest functionalCaseCommentRequest) {
        return functionalCaseCommentService.updateComment(functionalCaseCommentRequest, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{commentId}")
    @Operation(summary = "用例管理-功能用例-用例评论-删除评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ_COMMENT)
    public void deleteComment(@PathVariable String commentId) {
        functionalCaseCommentService.deleteComment(commentId, SessionUtils.getUserId());
    }

    @GetMapping("/get/list/{caseId}")
    @Operation(summary = "用例管理-功能用例-用例评论-获取用例评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_READ)
    @CheckOwner(resourceId = "#caseId", resourceType = "functional_case")
    public List<FunctionalCaseCommentDTO> getCommentList(@PathVariable String caseId) {
       return functionalCaseCommentService.getCommentList(caseId);
    }
}
