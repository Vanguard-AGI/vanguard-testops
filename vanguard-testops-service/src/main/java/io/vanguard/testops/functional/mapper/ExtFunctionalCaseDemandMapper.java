package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.FunctionalCaseDemand;
import io.vanguard.testops.functional.dto.FunctionalDemandDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtFunctionalCaseDemandMapper {

    List<FunctionalDemandDTO> selectParentDemandByKeyword(@Param("keyword") String keyword, @Param("caseId") String caseId);

    List<FunctionalCaseDemand> selectDemandByProjectId(@Param("projectId") String projectId, @Param("platform") String platform);

    List<String> selectDemandIdsByCaseId(@Param("caseId") String caseId, @Param("platform") String platform);


}
