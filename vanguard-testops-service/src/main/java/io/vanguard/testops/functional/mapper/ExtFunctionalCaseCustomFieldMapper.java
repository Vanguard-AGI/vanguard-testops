package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.FunctionalCaseCustomField;
import io.vanguard.testops.functional.dto.FunctionalCaseCustomFieldDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtFunctionalCaseCustomFieldMapper {


    List<FunctionalCaseCustomField> getCustomFieldByCaseIds(@Param("ids") List<String> ids);

    void batchUpdate(@Param("functionalCaseCustomField") FunctionalCaseCustomField functionalCaseCustomField, @Param("ids") List<String> ids);

    List<FunctionalCaseCustomFieldDTO> getCustomFieldsByCaseIds(@Param("ids") List<String> ids);

    void batchDelete(@Param("functionalCaseCustomField") FunctionalCaseCustomField functionalCaseCustomField, @Param("ids") List<String> ids);
}
