package io.vanguard.testops.api.support.parser.postman;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class PostmanKeyValue {
    private String key;
    private String value;
    private String type;
    private JsonNode description;
    private String contentType;
    private boolean disabled;

    public PostmanKeyValue() {
    }

    public PostmanKeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
