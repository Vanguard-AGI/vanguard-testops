package io.vanguard.testops.project.invoker;

import io.vanguard.testops.project.domain.FileAssociation;
import io.vanguard.testops.project.domain.FileMetadata;
import org.springframework.stereotype.Component;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:48
 */
@Component
public interface FileAssociationUpdateService {

    /**
     * 文件管理文件-用例最新文件时调用
     * @param originFileAssociation     原来的文件ID
     * @param newFileMetadata  最新文件
     */
    void handleUpgrade(FileAssociation originFileAssociation, FileMetadata newFileMetadata);
}
