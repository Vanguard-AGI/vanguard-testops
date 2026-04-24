package io.vanguard.testops.functional.dto;

import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseAttachment;
import io.vanguard.testops.functional.domain.FunctionalCaseBlob;
import io.vanguard.testops.functional.domain.FunctionalCaseCustomField;
import io.vanguard.testops.project.domain.FileAssociation;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class FunctionalCaseHistoryLogDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private FunctionalCase functionalCase;

    private FunctionalCaseBlob functionalCaseBlob;

    private List<FunctionalCaseCustomField> customFields;

    private List<FunctionalCaseAttachment> caseAttachments;

    private List<FileAssociation> fileAssociationList;

    public FunctionalCaseHistoryLogDTO(FunctionalCase functionalCase, FunctionalCaseBlob functionalCaseBlob, List<FunctionalCaseCustomField> customFields, List<FunctionalCaseAttachment> caseAttachments, List<FileAssociation> fileAssociationList) {
        this.functionalCase = functionalCase;
        this.functionalCaseBlob = functionalCaseBlob;
        this.customFields = customFields;
        this.caseAttachments = caseAttachments;
        this.fileAssociationList = fileAssociationList;
    }

    public FunctionalCaseHistoryLogDTO() {
    }
}
