package io.vanguard.testops.bug.service;

import io.vanguard.testops.bug.domain.BugRelationCase;
import io.vanguard.testops.bug.dto.response.BugRelateCaseDTO;
import io.vanguard.testops.bug.mapper.BugRelationCaseMapper;
import io.vanguard.testops.bug.mapper.ExtBugRelateCaseMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class BugRelateCaseLogService {

    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private ExtBugRelateCaseMapper extBugRelateCaseMapper;

    /**
     * 新增缺陷日志
     *
     * @param id 取消关联的引用ID
     * @return 日志
     */
    @SuppressWarnings("unused")
    public LogDTO getRelateLog(String id) {
        BugRelationCase bugRelationCase = bugRelationCaseMapper.selectByPrimaryKey(id);
        BugRelateCaseDTO relateCase = extBugRelateCaseMapper.getRelateCase(bugRelationCase.getCaseId(), bugRelationCase.getCaseType());
        LogDTO dto = new LogDTO(relateCase.getProjectId(), null, bugRelationCase.getBugId(), null, OperationLogType.DISASSOCIATE.name(), OperationLogModule.BUG_MANAGEMENT_INDEX, relateCase.getRelateCaseName());
        dto.setPath("/bug/un-relate");
        dto.setMethod(HttpMethodConstants.GET.name());
        dto.setModifiedValue(JSON.toJSONBytes(relateCase));
        return dto;
    }
}
