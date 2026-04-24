package io.vanguard.testops.api.support.parser.postman;

import lombok.Data;

@Data
public class PostmanResponse {

    private Integer code;
    private String name;
    private PostmanRequest originalRequest;

    //在要解析的postman文件中，response中的下面几个参数感觉没有用到。所以暂时注释
    //    private String status;
    //    private List<PostmanKeyValue> header;
    //    private JsonNode body;
}
