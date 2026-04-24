package io.vanguard.testops.functional.provider;

import io.vanguard.testops.dto.BaseCaseCustomFieldDTO;
import io.vanguard.testops.dto.TestCaseProviderDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseCustomFieldDTO;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseMapper;
import io.vanguard.testops.functional.service.FunctionalCaseService;
import io.vanguard.testops.provider.BaseAssociateCaseProvider;
import io.vanguard.testops.request.AssociateOtherCaseRequest;
import io.vanguard.testops.request.TestCasePageProviderRequest;
import io.vanguard.testops.sdk.util.BeanUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("FUNCTIONAL")
public class AssociateFunctionalProvider implements BaseAssociateCaseProvider {

    @Resource
    private FunctionalCaseService functionalCaseService;
    @Resource
    private ExtFunctionalCaseMapper extFunctionalCaseMapper;

    @Override
    public List<TestCaseProviderDTO> listUnRelatedTestCaseList(TestCasePageProviderRequest testCasePageProviderRequest) {
        List<TestCaseProviderDTO> functionalCases = extFunctionalCaseMapper.listUnRelatedCaseWithBug(testCasePageProviderRequest, false, testCasePageProviderRequest.getSortString());
        if (CollectionUtils.isEmpty(functionalCases)) {
            return new ArrayList<>();
        }
        List<String> ids = functionalCases.stream().map(TestCaseProviderDTO::getId).toList();
        Map<String, List<FunctionalCaseCustomFieldDTO>> caseCustomFiledMap = functionalCaseService.getCaseCustomFiledMap(ids, testCasePageProviderRequest.getProjectId());
        functionalCases.forEach(functionalCase -> {
            List<FunctionalCaseCustomFieldDTO> customFields = caseCustomFiledMap.get(functionalCase.getId());
            if (CollectionUtils.isNotEmpty(customFields)) {
                List<BaseCaseCustomFieldDTO> customs = new ArrayList<>();
                for (FunctionalCaseCustomFieldDTO customField : customFields) {
                    BaseCaseCustomFieldDTO baseCaseCustomFieldDTO = new BaseCaseCustomFieldDTO();
                    BeanUtils.copyBean(baseCaseCustomFieldDTO, customField);
                    customs.add(baseCaseCustomFieldDTO);
                }
                functionalCase.setCustomFields(customs);
            }
        });
        return functionalCases;
    }

    @Override
    public List<String> getRelatedIdsByParam(AssociateOtherCaseRequest request, boolean deleted) {
        if (request.isSelectAll()) {
            List<String> relatedIds = extFunctionalCaseMapper.getSelectIdsByAssociateParam(request, deleted);
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                relatedIds = relatedIds.stream().filter(id -> !request.getExcludeIds().contains(id)).toList();
            }
            return relatedIds;
        } else {
            return request.getSelectIds();
        }
    }
}
