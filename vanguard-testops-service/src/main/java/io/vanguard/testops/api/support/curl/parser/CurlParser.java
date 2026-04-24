package io.vanguard.testops.api.support.curl.parser;

import io.vanguard.testops.api.support.curl.domain.CurlEntity;
import io.vanguard.testops.api.support.curl.handler.CurlHandlerChain;
import io.vanguard.testops.api.support.curl.handler.HeaderHandler;
import io.vanguard.testops.api.support.curl.handler.HttpBodyHandler;
import io.vanguard.testops.api.support.curl.handler.HttpMethodHandler;
import io.vanguard.testops.api.support.curl.handler.ICurlHandler;
import io.vanguard.testops.api.support.curl.handler.QueryParamsHandler;
import io.vanguard.testops.api.support.curl.handler.UrlPathHandler;

public class CurlParser {

    /**
     * 解析 curl 工具类
     */
    public static CurlEntity parse(String curl) {
        CurlEntity entity = CurlEntity.builder().build();
        ICurlHandler<CurlEntity, String> handlerChain = CurlHandlerChain.init();

        handlerChain.next(new UrlPathHandler())
                .next(new QueryParamsHandler())
                .next(new HttpMethodHandler())
                .next(new HeaderHandler())
                .next(new HttpBodyHandler());

        handlerChain.handle(entity, curl);
        return entity;
    }
}
