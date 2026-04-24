package io.vanguard.testops.api.support.parser.postman;

import lombok.Data;

import java.util.List;

@Data
public class PostmanItem {
    private String name;
    private PostmanRequest request;
    private List<PostmanResponse> response;
    private List<PostmanItem> item;
    private ProtocolProfileBehavior protocolProfileBehavior;

    @Data
    public class ProtocolProfileBehavior {
        private Boolean followRedirects = true;
    }
}
