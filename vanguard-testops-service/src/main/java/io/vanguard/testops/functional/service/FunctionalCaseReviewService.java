package io.vanguard.testops.functional.service;


import io.vanguard.testops.functional.dto.CaseReviewHistoryDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseReviewDTO;
import io.vanguard.testops.functional.mapper.ExtCaseReviewFunctionalCaseMapper;
import io.vanguard.testops.functional.mapper.ExtCaseReviewHistoryMapper;
import io.vanguard.testops.functional.request.FunctionalCaseReviewListRequest;
import io.vanguard.testops.sdk.constants.UserRoleScope;
import io.vanguard.testops.sdk.util.Translator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * 功能用例和其他用例的中间表服务实现类
 *
 * @date : 2026-04-22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FunctionalCaseReviewService {

    @Resource
    private ExtCaseReviewFunctionalCaseMapper extCaseReviewFunctionalCaseMapper;

    @Resource
    private ExtCaseReviewHistoryMapper extCaseReviewHistoryMapper;

    public List<FunctionalCaseReviewDTO> getFunctionalCaseReviewPage(FunctionalCaseReviewListRequest request) {
       return extCaseReviewFunctionalCaseMapper.list(request);
    }

    public List<CaseReviewHistoryDTO> getCaseReviewHistory(String caseId) {
        List<CaseReviewHistoryDTO> list = extCaseReviewHistoryMapper.getHistoryListWidthCaseId(caseId, null);
        for (CaseReviewHistoryDTO caseReviewHistoryDTO : list) {
            if (StringUtils.equalsIgnoreCase(caseReviewHistoryDTO.getCreateUser(), UserRoleScope.SYSTEM)) {
                caseReviewHistoryDTO.setUserName(Translator.get("case_review_history.system"));
            }
            if (caseReviewHistoryDTO.getContent() != null) {
                caseReviewHistoryDTO.setContentText(new String(caseReviewHistoryDTO.getContent(), StandardCharsets.UTF_8));
            }
        }
        return list;
    }
}