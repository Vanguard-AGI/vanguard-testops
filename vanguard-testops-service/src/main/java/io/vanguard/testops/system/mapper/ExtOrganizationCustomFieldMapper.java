package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.dto.sdk.OptionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtOrganizationCustomFieldMapper {
    List<String> getCustomFieldByRefId(@Param("refId") String refId);

    List<OptionDTO> getCustomFieldOptions(@Param("ids") List<String> ids);
}
