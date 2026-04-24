package io.vanguard.testops.system.dto;

import lombok.Data;

@Data
public class PageConfigResponse {
    private String paramKey;
    private String paramValue;
    private String type;
    private String file;
    private String fileName;
}
