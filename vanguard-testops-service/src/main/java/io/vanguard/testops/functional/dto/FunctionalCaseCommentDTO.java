package io.vanguard.testops.functional.dto;

import io.vanguard.testops.functional.domain.FunctionalCaseComment;
import io.vanguard.testops.system.dto.CommentUserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FunctionalCaseCommentDTO extends FunctionalCaseComment {

    @Schema(description = "评论相关用户信息")
    private List<CommentUserInfo> commentUserInfos;

    @Schema(description =  "该条评论下的所有回复数据")
    private List<FunctionalCaseCommentDTO> childComments;

}
