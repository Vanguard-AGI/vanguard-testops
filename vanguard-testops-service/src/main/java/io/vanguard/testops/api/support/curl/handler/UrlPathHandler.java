package io.vanguard.testops.api.support.curl.handler;

import io.vanguard.testops.api.support.curl.constants.CurlPatternConstants;
import io.vanguard.testops.api.support.curl.domain.CurlEntity;

import java.util.regex.Matcher;

public class UrlPathHandler extends CurlHandlerChain {

    @Override
    public void handle(CurlEntity entity, String curl) {
        String url = parseUrlPath(curl);
        entity.setUrl(url);
        super.nextHandle(entity, curl);
    }

    /**
     * url路径解析
     */
    private String parseUrlPath(String curl) {
        Matcher matcher = CurlPatternConstants.URL_PATH_PATTERN.matcher(curl);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
        }
        return null;
    }
}
