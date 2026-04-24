package io.vanguard.testops.bug.dto.response;

import io.vanguard.testops.bug.domain.BugComment;
import io.vanguard.testops.system.dto.CommentUserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class BugCommentDTO extends BugComment {

    @Schema(description = "评论相关用户信息")
    private List<CommentUserInfo> commentUserInfos;

    @Schema(description = "子评论")
    private List<BugCommentDTO> childComments;
}
