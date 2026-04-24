package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.FunctionalCaseModuleExample;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseMapper;
import io.vanguard.testops.functional.mapper.FunctionalCaseModuleMapper;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author Jan
 */
@Component
public class CleanupFunctionalCaseResourceService implements CleanupProjectResourceService {

    @Resource
    private ExtFunctionalCaseMapper extFunctionalCaseMapper;

    @Resource
    private DeleteFunctionalCaseService deleteFunctionalCaseService;

    @Resource
    private FunctionalCaseModuleMapper functionalCaseModuleMapper;


    @Override
    public void deleteResources(String projectId) {
        List<String> ids = extFunctionalCaseMapper.getFunctionalCaseIds(projectId);
        if (CollectionUtils.isNotEmpty(ids)) {
            deleteFunctionalCaseService.deleteFunctionalCaseResource(ids, projectId);
        }
        //删除模块
        FunctionalCaseModuleExample functionalCaseModuleExample = new FunctionalCaseModuleExample();
        functionalCaseModuleExample.createCriteria().andProjectIdEqualTo(projectId);
        functionalCaseModuleMapper.deleteByExample(functionalCaseModuleExample);
    }
    
}
