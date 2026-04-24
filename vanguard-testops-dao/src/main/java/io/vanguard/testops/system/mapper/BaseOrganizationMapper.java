package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.Organization;

import java.util.List;

public interface BaseOrganizationMapper {

    List<Organization> selectOrganizationByIdList(List<String> organizationIds);
}
