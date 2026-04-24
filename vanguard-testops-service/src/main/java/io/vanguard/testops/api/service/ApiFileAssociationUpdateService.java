package io.vanguard.testops.api.service;

import io.vanguard.testops.api.service.debug.ApiDebugService;
import io.vanguard.testops.api.service.definition.ApiDefinitionService;
import io.vanguard.testops.api.service.definition.ApiTestCaseService;
import io.vanguard.testops.api.service.scenario.ApiScenarioService;
import io.vanguard.testops.project.domain.FileAssociation;
import io.vanguard.testops.project.domain.FileMetadata;
import io.vanguard.testops.project.invoker.FileAssociationUpdateService;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.FileAssociationSourceUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:48
 */
@Service
public class ApiFileAssociationUpdateService implements FileAssociationUpdateService {

    /**
     * 处理接口关联的文件被更新
     *
     * @param originFileAssociation 原来的文件ID
     * @param newFileMetadata       最新文件
     */
    @Override
    public void handleUpgrade(FileAssociation originFileAssociation, FileMetadata newFileMetadata) {
        String sourceType = originFileAssociation.getSourceType();
        switch (sourceType) {
            case FileAssociationSourceUtil.SOURCE_TYPE_API_DEBUG ->
                    Objects.requireNonNull(CommonBeanFactory.getBean(ApiDebugService.class))
                            .handleFileAssociationUpgrade(originFileAssociation, newFileMetadata);
            case FileAssociationSourceUtil.SOURCE_TYPE_API_DEFINITION ->
                    Objects.requireNonNull(CommonBeanFactory.getBean(ApiDefinitionService.class))
                            .handleFileAssociationUpgrade(originFileAssociation, newFileMetadata);
            case FileAssociationSourceUtil.SOURCE_TYPE_API_TEST_CASE ->
                    Objects.requireNonNull(CommonBeanFactory.getBean(ApiTestCaseService.class))
                            .handleFileAssociationUpgrade(originFileAssociation, newFileMetadata);
            case FileAssociationSourceUtil.SOURCE_TYPE_API_SCENARIO_STEP ->
                    Objects.requireNonNull(CommonBeanFactory.getBean(ApiScenarioService.class))
                            .handleStepFileAssociationUpgrade(originFileAssociation, newFileMetadata);
            case FileAssociationSourceUtil.SOURCE_TYPE_API_SCENARIO ->
                    Objects.requireNonNull(CommonBeanFactory.getBean(ApiScenarioService.class))
                            .handleScenarioFileAssociationUpgrade(originFileAssociation, newFileMetadata);
            default -> {
            }
        }
    }

}
