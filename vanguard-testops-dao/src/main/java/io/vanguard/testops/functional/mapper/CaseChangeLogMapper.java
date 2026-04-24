package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.functional.domain.CaseChangeLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CaseChangeLogMapper {
    int insert(CaseChangeLog record);

    CaseChangeLog selectByPrimaryKey(Long id);

    List<CaseChangeLog> selectByCaseId(String caseId);
}
