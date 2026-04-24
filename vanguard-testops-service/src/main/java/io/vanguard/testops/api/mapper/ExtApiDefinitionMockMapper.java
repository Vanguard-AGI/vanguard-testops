package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiDefinitionMock;
import io.vanguard.testops.api.dto.definition.ApiDefinitionMockDTO;
import io.vanguard.testops.api.dto.definition.ApiMockWithBlob;
import io.vanguard.testops.api.dto.definition.ApiTestCaseBatchRequest;
import io.vanguard.testops.api.dto.definition.request.ApiDefinitionMockPageRequest;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApiDefinitionMockMapper {

    @BaseConditionFilter
    List<ApiDefinitionMockDTO> list(@Param("request") ApiDefinitionMockPageRequest request);

    List<String> getIdsByApiIds(@Param("ids") List<String> ids);

    @BaseConditionFilter
    List<String> getIds(@Param("request") ApiTestCaseBatchRequest request);

    List<ApiDefinitionMock> getTagsByIds(@Param("ids") List<String> ids);

    List<ApiDefinitionMock> getMockInfoByIds(@Param("ids") List<String> ids);

    List<ApiMockWithBlob> selectAllDetailByApiIds(@Param("apiIds") List<String> apiIds);
}
