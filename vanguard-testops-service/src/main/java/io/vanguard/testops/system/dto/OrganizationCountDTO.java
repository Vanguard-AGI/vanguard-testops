package io.vanguard.testops.system.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Jan
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrganizationCountDTO {

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 项目数量
     */
    private Integer projectCount;

    /**
     * 组织ID
     */
    private String id;
}
