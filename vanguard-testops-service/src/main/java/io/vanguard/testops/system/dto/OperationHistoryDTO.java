package io.vanguard.testops.system.dto;

import io.vanguard.testops.system.domain.OperationHistory;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class OperationHistoryDTO extends OperationHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 操作人
    private String createUserName;

    // 版本
    private String versionName;

    //是否是最新
    private boolean isLatest;
}
