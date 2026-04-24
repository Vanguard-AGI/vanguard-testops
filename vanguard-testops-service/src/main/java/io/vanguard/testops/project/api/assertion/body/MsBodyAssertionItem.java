package io.vanguard.testops.project.api.assertion.body;

import lombok.Data;

/**
 * body 断言基类
 * @Author: Jan
 * @CreateTime: 2023-11-23  14:25
 */
@Data
public abstract class MsBodyAssertionItem {
    /**
     * 是否启用
     * 默认启用
     */
    private Boolean enable = true;
}
