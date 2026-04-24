package io.vanguard.testops.functional.support.importexport.excel.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Jan
 */
@Data
public class FunctionalCaseHeader implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段英文名称")
    private String id;
    @Schema(description = "字段中文名称")
    private String name;
}
