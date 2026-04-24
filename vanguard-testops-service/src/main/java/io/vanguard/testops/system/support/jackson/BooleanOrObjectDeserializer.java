package io.vanguard.testops.system.support.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class BooleanOrObjectDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();

        if (token == JsonToken.VALUE_TRUE) {
            return true;
        } else if (token == JsonToken.VALUE_FALSE) {
            return false;
        } else if (token == JsonToken.START_OBJECT) {
            p.skipChildren();
            return true;
        } else if (token == JsonToken.VALUE_NULL) {
            return false;
        }

        return false;
    }
}
