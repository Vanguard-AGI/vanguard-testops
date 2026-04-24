package io.vanguard.testops.system.dto.taskcenter.request;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.vanguard.testops.system.dto.taskcenter.enums.ScheduleTagType;
import io.vanguard.testops.sdk.valid.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class TaskCenterSchedulePageRequest extends BasePageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description =  "定时任务所属类别", requiredMode = Schema.RequiredMode.REQUIRED)
    @EnumValue(enumClass = ScheduleTagType.class)
    private String scheduleTagType = ScheduleTagType.API_IMPORT.toString();
}
