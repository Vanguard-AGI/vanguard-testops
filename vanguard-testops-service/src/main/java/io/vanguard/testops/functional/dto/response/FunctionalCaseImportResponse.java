package io.vanguard.testops.functional.dto.response;

import io.vanguard.testops.functional.support.importexport.excel.domain.FunctionalCaseExcelData;
import io.vanguard.testops.system.excel.domain.ExcelErrData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Jan
 */
@Data
public class FunctionalCaseImportResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成功数量")
    private int successCount;
    @Schema(description = "失败数量")
    private int failCount;
    @Schema(description = "报错信息")
    private List<ExcelErrData<FunctionalCaseExcelData>> errorMessages;

}
