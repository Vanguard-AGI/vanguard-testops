package io.vanguard.testops.system.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vanguard.testops.system.support.jackson.BooleanOrObjectDeserializer;

@Data
public class PageConfigRequest {
    private String paramKey;
    private String paramValue;
    private String type;
    private String fileName;
    private Boolean original;
    
    @JsonDeserialize(using = BooleanOrObjectDeserializer.class)
    private Boolean hasFile;
}
