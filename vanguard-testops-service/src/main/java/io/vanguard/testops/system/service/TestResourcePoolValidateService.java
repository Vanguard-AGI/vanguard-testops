package io.vanguard.testops.system.service;

import io.vanguard.testops.system.dto.pool.TestResourceNodeDTO;

import java.util.List;

public interface TestResourcePoolValidateService {

    void validateNodeList(List<TestResourceNodeDTO> nodesList);

}
