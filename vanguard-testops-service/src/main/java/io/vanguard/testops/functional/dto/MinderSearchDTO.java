package io.vanguard.testops.functional.dto;

import io.vanguard.testops.functional.domain.FunctionalCaseBlob;
import io.vanguard.testops.system.dto.sdk.BaseTreeNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MinderSearchDTO {
    private List<BaseTreeNode> baseTreeNodes;
    private Map<String, List<ReviewFunctionalCaseDTO>> moduleCaseMap;
    private Map<String, FunctionalCaseBlob> blobMap;
    private Map<String, List<FunctionalCaseCustomFieldDTO>> customFieldMap;
    private String reviewPassRule;
    private boolean viewResult;
    private boolean viewFlag;
    private BaseTreeNode baseTreeNode;
}
