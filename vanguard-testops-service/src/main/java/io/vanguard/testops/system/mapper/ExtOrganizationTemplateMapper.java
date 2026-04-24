package io.vanguard.testops.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jan
 */
public interface ExtOrganizationTemplateMapper {
    List<String> getTemplateIdByRefId(@Param("refId") String refId);
}
