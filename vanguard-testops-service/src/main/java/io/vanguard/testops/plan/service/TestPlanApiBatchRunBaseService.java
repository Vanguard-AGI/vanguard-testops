package io.vanguard.testops.plan.service;

import io.vanguard.testops.plan.domain.TestPlanCollection;
import io.vanguard.testops.plan.domain.TestPlanCollectionExample;
import io.vanguard.testops.plan.mapper.TestPlanCollectionMapper;
import io.vanguard.testops.sdk.constants.CommonConstants;
import io.vanguard.testops.sdk.dto.api.task.ApiRunModeConfigDTO;
import io.vanguard.testops.sdk.dto.api.task.ApiRunRetryConfig;
import io.vanguard.testops.sdk.util.BeanUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanApiBatchRunBaseService {

    @Resource
    private TestPlanCollectionMapper testPlanCollectionMapper;

    public ApiRunModeConfigDTO getApiRunModeConfig(String collectionId) {
        TestPlanCollection collection = testPlanCollectionMapper.selectByPrimaryKey(collectionId);
        return getApiRunModeConfig(collection);
    }

    public ApiRunModeConfigDTO getApiRunModeConfig(TestPlanCollection collection) {
        if (collection == null) {
            ApiRunModeConfigDTO runModeConfig = new ApiRunModeConfigDTO();
            runModeConfig.setPoolId(StringUtils.EMPTY);
            return runModeConfig;
        }
        TestPlanCollection rootCollection = getExtendedRootCollection(collection);
        ApiRunModeConfigDTO runModeConfig = getApiRunModeConfig(rootCollection, collection);
        return runModeConfig;
    }

    public TestPlanCollection getExtendedRootCollection(TestPlanCollection collection) {
        if (BooleanUtils.isTrue(collection.getExtended())
                && !StringUtils.equals(collection.getParentId(), CommonConstants.DEFAULT_NULL_VALUE)) {
            TestPlanCollectionExample example = new TestPlanCollectionExample();
            example.createCriteria().andIdEqualTo(collection.getParentId());
            return testPlanCollectionMapper.selectByExample(example)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public ApiRunModeConfigDTO getApiRunModeConfig(TestPlanCollection rootCollection, TestPlanCollection collection) {
        if (rootCollection != null && BooleanUtils.isTrue(collection.getExtended())) {
            // 如果是继承，则使用根节点的配置
            collection = rootCollection;
        }
        ApiRunModeConfigDTO runModeConfig = BeanUtils.copyBean(new ApiRunModeConfigDTO(), collection);
        runModeConfig.setPoolId(collection.getTestResourcePoolId());
        runModeConfig.setStopOnFailure(collection.getStopOnFail());
        runModeConfig.setRunMode(collection.getExecuteMethod());
        if (BooleanUtils.isTrue(runModeConfig.getRetryOnFail())) {
            ApiRunRetryConfig retryConfig = BeanUtils.copyBean(new ApiRunRetryConfig(), collection);
            runModeConfig.setRetryConfig(retryConfig);
        }
        return runModeConfig;
    }
}
