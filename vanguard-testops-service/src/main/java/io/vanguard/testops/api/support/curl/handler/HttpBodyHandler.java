package io.vanguard.testops.api.support.curl.handler;

import io.vanguard.testops.api.support.curl.constants.CurlPatternConstants;
import io.vanguard.testops.api.support.curl.domain.CurlEntity;
import io.vanguard.testops.api.dto.request.http.body.Body;
import io.vanguard.testops.api.support.format.json.JSONUtil;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class HttpBodyHandler extends CurlHandlerChain {
    @Override
    public void handle(CurlEntity entity, String curl) {
        parseBody(curl, entity);
        super.nextHandle(entity, curl);
    }

    /**
     * 请求体解析
     */
    private void parseBody(String curl, CurlEntity entity) {
        Matcher formMatcher = CurlPatternConstants.HTTP_FROM_BODY_PATTERN.matcher(curl);
        if (formMatcher.find()) {
            entity.setBodyType(Body.BodyType.FORM_DATA.name());
            entity.setBody(parseFormBody(formMatcher));
        }

        Matcher urlencodeMatcher = CurlPatternConstants.HTTP_URLENCODE_BODY_PATTERN.matcher(curl);
        if (urlencodeMatcher.find()) {
            entity.setBodyType(Body.BodyType.WWW_FORM.name());
            entity.setBody(parseUrlEncodeBody(urlencodeMatcher));
        }

        Matcher xmlJsonMatcher = CurlPatternConstants.HTTP_XML_JSON_BODY_PATTERN.matcher(curl);
        if (xmlJsonMatcher.find()) {
            entity.setBody(parseRowBody(xmlJsonMatcher, entity));
        }

        Matcher rawMatcher = CurlPatternConstants.HTTP_ROW_BODY_PATTERN.matcher(curl);
        if (rawMatcher.find()) {
            entity.setBodyType(Body.BodyType.RAW.name());
            entity.setBody(rawMatcher.group(1));
        }

        Matcher defaultMatcher = CurlPatternConstants.DEFAULT_HTTP_BODY_PATTERN.matcher(curl);
        if (defaultMatcher.find()) {
            entity.setBody(parseDefaultBody(defaultMatcher, entity));
        }
    }

    private Object parseDefaultBody(Matcher defaultMatcher, CurlEntity entity) {
        String bodyStr = "";
        if (defaultMatcher.group(1) != null) {
            // 单引号数据
            bodyStr = defaultMatcher.group(1);
        } else if (defaultMatcher.group(2) != null) {
            // 双引号数据
            bodyStr = defaultMatcher.group(2);
        } else {
            // 无引号数据
            bodyStr = defaultMatcher.group(3);
        }

        if (isJSON(bodyStr)) {
            entity.setBodyType(Body.BodyType.JSON.name());
            return JSON.parseMap(bodyStr);
        }
        if (isXML(bodyStr)) {
            entity.setBodyType(Body.BodyType.XML.name());
            return bodyStr;
        }

        // 其他格式 a=b&c=d
        entity.setBodyType(Body.BodyType.WWW_FORM.name());
        Matcher kvMatcher = CurlPatternConstants.DEFAULT_HTTP_BODY_PATTERN_KV.matcher(bodyStr);
        return kvMatcher.matches() ? parseKVBody(bodyStr) : new HashMap<>();
    }

    private Map<String, Object> parseKVBody(String kvBodyStr) {
        Map<String, Object> map = new HashMap<>();
        String[] pairs = kvBodyStr.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    private Map<String, Object> parseFormBody(Matcher formMatcher) {
        Map<String, Object> formData = new HashMap<>();
        formMatcher.reset();
        while (formMatcher.find()) {
            // 提取表单
            String formItem = formMatcher.group(1) != null ? formMatcher.group(1) : formMatcher.group(2);
            String[] keyValue = formItem.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                // 文件属性
                if (value.startsWith("@")) {
                    // 获取文件名
                    formData.put(key, value.substring(1));
                } else {
                    formData.put(key, value);
                }
            }
        }
        return formData;
    }

    private Map<String, Object> parseUrlEncodeBody(Matcher urlencodeMatcher) {
        Map<String, Object> urlEncodeData = new HashMap<>();
        urlencodeMatcher.reset();
        while (urlencodeMatcher.find()) {
            String keyValueEncoded = urlencodeMatcher.group(1);
            String[] keyValue = keyValueEncoded.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8);
                urlEncodeData.put(key, decodedValue);
            }
        }
        return urlEncodeData;
    }

    private Object parseRowBody(Matcher rowMatcher, CurlEntity entity) {
        String rawData = rowMatcher.group(1);

        if (isXML(rawData)) {
            entity.setBodyType(Body.BodyType.XML.name());
            return rawData;
        }

        if (isJSON(rawData)) {
            entity.setBodyType(Body.BodyType.JSON.name());
            return JSON.parseMap(rawData);
        }

        try {
            return parseDefaultBody(rowMatcher, entity);
        } catch (Exception e) {
            throw new MSException(Translator.get("curl_raw_content_is_invalid"), e);
        }
    }

    private boolean isJSON(String jsonStr) {
        try {
            JSONUtil.parseObject(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isXML(String xmlStr) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlStr));
            builder.parse(is);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
