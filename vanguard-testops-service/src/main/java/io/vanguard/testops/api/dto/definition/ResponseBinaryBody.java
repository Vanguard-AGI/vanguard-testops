package io.vanguard.testops.api.dto.definition;

import io.vanguard.testops.api.dto.ApiFile;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class ResponseBinaryBody {
    private boolean sendAsBody;
    private String description;
    /**
     * 文件对象
     */
    @Valid
    private ApiFile file;
}
