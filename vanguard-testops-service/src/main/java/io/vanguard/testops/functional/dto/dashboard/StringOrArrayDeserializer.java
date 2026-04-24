package io.vanguard.testops.functional.dto.dashboard;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 支持 JSON 中 personal 为字符串或字符串数组，统一反序列化为 List&lt;String&gt;
 */
public class StringOrArrayDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_STRING) {
            String s = p.getText();
            return (s == null || s.isBlank()) ? Collections.emptyList() : Collections.singletonList(s.trim());
        }
        if (token == JsonToken.START_ARRAY) {
            List<String> list = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                    String s = p.getText();
                    if (s != null && !s.isBlank()) {
                        list.add(s.trim());
                    }
                }
            }
            return list;
        }
        return null;
    }
}
