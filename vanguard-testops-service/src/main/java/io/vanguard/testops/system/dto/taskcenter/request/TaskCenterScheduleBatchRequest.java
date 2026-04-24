package io.vanguard.testops.system.dto.taskcenter.request;

import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.vanguard.testops.system.dto.taskcenter.enums.ScheduleTagType;
import io.vanguard.testops.sdk.valid.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TaskCenterScheduleBatchRequest extends TableBatchProcessDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属模块", requiredMode = Schema.RequiredMode.REQUIRED)
    @EnumValue(enumClass = ScheduleTagType.class)
    private String scheduleTagType = ScheduleTagType.API_IMPORT.toString();

}
