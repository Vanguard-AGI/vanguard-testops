package io.vanguard.testops.system.notice.sender.client;

import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Map;

public class ClientPost {

    public static void executeClient(String webhook, CloseableHttpClient httpClient, Map<String, Object> mp) {
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(webhook);
            StringEntity entity = new StringEntity(JSON.toJSONString(mp), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
        } catch (Exception e) {
            LogUtils.error(e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                LogUtils.error(e);
            }
        }
    }
}
