package io.vanguard.testops.api.support.curl.handler;

import io.vanguard.testops.api.support.curl.domain.CurlEntity;

public interface ICurlHandler<R, S> {

    ICurlHandler<CurlEntity, String> next(ICurlHandler<CurlEntity, String> handler);

    void handle(CurlEntity entity, String curl);
}
