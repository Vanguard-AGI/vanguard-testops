package io.vanguard.testops.project.dto.filemanagement;

import lombok.Data;

@Data
public class FileAssociationSource {
    private String sourceId;
    private String sourceNum;
    private String sourceName;
    private String redirectId;
}
