package io.vanguard.testops.project.invoker;

import io.vanguard.testops.project.domain.FileAssociation;
import io.vanguard.testops.project.domain.FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-20  18:55
 */
@Component
public class FileAssociationUpdateServiceInvoker implements FileAssociationUpdateService {

    private final List<FileAssociationUpdateService> fileAssociationUpdateServices;

    @Autowired
    public FileAssociationUpdateServiceInvoker(List<FileAssociationUpdateService> services) {
        this.fileAssociationUpdateServices = services;
    }

    @Override
    public void handleUpgrade(FileAssociation originFileAssociation, FileMetadata newFileMetadata) {
        this.fileAssociationUpdateServices.forEach(service -> service.handleUpgrade(originFileAssociation, newFileMetadata));
    }
}
