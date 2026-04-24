package io.vanguard.testops.bug.mapper;

import io.vanguard.testops.bug.domain.BugLocalAttachment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtBugLocalAttachmentMapper {

    /**
     * 批量插入缺陷附件关系
     * @param attachments 缺陷附件集合
     */
    void batchInsert(@Param("list") List<BugLocalAttachment> attachments);
}
