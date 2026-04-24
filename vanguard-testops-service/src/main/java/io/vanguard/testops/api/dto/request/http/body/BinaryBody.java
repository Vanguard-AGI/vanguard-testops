package io.vanguard.testops.api.dto.request.http.body;

import io.vanguard.testops.api.dto.ApiFile;
import jakarta.validation.Valid;
import lombok.Data;

/**
 * binary 请求体
 * @Author: Jan
 * @CreateTime: 2023-11-06  18:25
 */
@Data
public class BinaryBody {
    /**
     *  描述
     */
    private String description;
    /**
     * 文件对象
     */
    @Valid
    private ApiFile file;
}
