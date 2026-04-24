package io.vanguard.testops.api.dto.request.http.body;

import lombok.Data;

/**
 * raw 请求体
 * @Author: Jan
 * @CreateTime: 2023-11-06  18:25
 */
@Data
public class RawBody {
    /**
     *  请求体值
     */
    private String value;
}
