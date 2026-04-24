package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.CaseReview;
import io.vanguard.testops.functional.domain.CaseReviewExample;
import io.vanguard.testops.functional.domain.CaseReviewModuleExample;
import io.vanguard.testops.functional.mapper.CaseReviewMapper;
import io.vanguard.testops.functional.mapper.CaseReviewModuleMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CleanupCaseReviewResourceService implements CleanupProjectResourceService {
    @Resource
    private DeleteCaseReviewService deleteCaseReviewService;
    @Resource
    private CaseReviewModuleMapper caseReviewModuleMapper;
    @Resource
    private CaseReviewMapper caseReviewMapper;

    @Override
    public void deleteResources(String projectId) {
        LogUtils.info("删除当前项目[" + projectId + "]相关用例评审资源");
        CaseReviewExample caseReviewExample = new CaseReviewExample();
        caseReviewExample.createCriteria().andProjectIdEqualTo(projectId);
        List<CaseReview> caseReviews = caseReviewMapper.selectByExample(caseReviewExample);
        List<String> ids = caseReviews.stream().map(CaseReview::getId).toList();
        if (CollectionUtils.isNotEmpty(ids)) {
            deleteCaseReviewService.deleteCaseReviewResource(ids, projectId);
        }
        //删除模块
        CaseReviewModuleExample caseReviewModuleExample = new CaseReviewModuleExample();
        caseReviewModuleExample.createCriteria().andProjectIdEqualTo(projectId);
        caseReviewModuleMapper.deleteByExample(caseReviewModuleExample);
    }

}
