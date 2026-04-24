package io.vanguard.testops.api.support.parser.postman;

import lombok.Data;

@Data
public class PostmanCollectionInfo {
    private String postmanId;
    private String name;
    private String schema;
}
