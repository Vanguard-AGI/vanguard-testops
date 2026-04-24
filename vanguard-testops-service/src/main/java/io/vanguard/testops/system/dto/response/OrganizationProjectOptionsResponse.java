package io.vanguard.testops.system.dto.response;

import io.vanguard.testops.system.dto.OrganizationProjectOptionsDTO;
import lombok.Data;

import java.util.List;

@Data
public class OrganizationProjectOptionsResponse {

    List<OrganizationProjectOptionsDTO> organizationList;
    List<OrganizationProjectOptionsDTO> projectList;
}
