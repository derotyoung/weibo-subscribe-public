package com.derotyoung.util;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OkHttpClientUtil {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:%s.0) Gecko/20100101 Firefox/%s.0";

    public static final String FIREFOX_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0";

    public static final String EDGE_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 Edg/107.0.1418.35";

    public static final String CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36";

    private OkHttpClientUtil() {
    }

    public static String getUserAgent(String model) {
        int number = getRandomNumberInRange(99, 106);
        String firefoxUserAgent = String.format(model, number, number);

        List<String> list = List.of(firefoxUserAgent, EDGE_USER_AGENT, CHROME_USER_AGENT);
        return list.get(getRandomNumberInRange(0, 2));
    }

    public static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static OkHttpClient client(String httpProxy) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(75, TimeUnit.SECONDS)
                .writeTimeout(75, TimeUnit.SECONDS)
                .readTimeout(75, TimeUnit.SECONDS);

        if (StringUtils.hasText(httpProxy)) {
            HttpHost httpHost = HttpHost.create(httpProxy);
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpHost.getHostname(), httpHost.getPort())));
        }

        return builder.build();
    }

    public static String getRequestUrl(String baseUrl, Map<String, String> paramsMap) {
        StringBuilder reqUrl = new StringBuilder(baseUrl);
        if (paramsMap != null && !paramsMap.isEmpty()) {
            StringJoiner stringJoiner = new StringJoiner("&");
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                stringJoiner.add(entry.getKey() + "=" + entry.getValue());
            }
            reqUrl.append("?").append(stringJoiner);
        }

        return reqUrl.toString();
    }

    public static String requestGet(String url) throws IOException {
        Headers headers = weiboHeaders(getHost(url));
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .build();

        OkHttpClient client = client(null);
        Response response = client.newCall(request).execute();

        return Objects.requireNonNull(response.body()).string();
    }

    public static String requestGetWithSleep(String url) throws IOException {
        getRandomNumberInRange(500, 1500);
        return requestGet(url);
    }

    public static int requestGet(String url, String proxy) throws IOException {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json; charset=UTF-8");
        headers.add("User-Agent", FIREFOX_USER_AGENT);

        Request request = new Request.Builder()
                .url(url)
                .headers(headers.build())
                .build();

        OkHttpClient client = client(proxy);
        Response response = client.newCall(request).execute();

        return Objects.requireNonNull(response).code();
    }

    public static Response request(String url) throws IOException {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json; charset=UTF-8");
        String host = getHost(url);
        if (StringUtils.hasLength(host)) {
            headers.add("Host", host);
        }
        headers.add("User-Agent", FIREFOX_USER_AGENT);

        Request request = new Request.Builder()
                .url(url)
                .headers(headers.build())
                .build();

        OkHttpClient client = client(null);
        return client.newCall(request).execute();
    }

    private static String getHost(String url) {
        String baseDomain = null;
        if (StringUtils.hasText(url)) {
            final int schemeIdx = url.indexOf("://");
            if (schemeIdx > 0) {
                String text = url.substring(schemeIdx + 3);
                final int spIdx = text.indexOf("/");
                if (spIdx > 0) {
                    baseDomain = text.substring(0, spIdx);
                } else {
                    final int spIdx2 = text.indexOf("?");
                    if (spIdx2 > 0) {
                        baseDomain = text.substring(0, spIdx2);
                    } else {
                        baseDomain = text;
                    }
                }
            }
        }
        return baseDomain;
    }

    public static Headers weiboHeaders(String host) {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json; charset=UTF-8");
        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        // headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("Accept-Language", "en-US,zh-CN;q=0.5");
        headers.add("Connection", "keep-alive");
        if (StringUtils.hasLength(host)) {
            headers.add("Host", host);
        }
        headers.add("Sec-Fetch-Dest", "document");
        headers.add("Sec-Fetch-Mode", "navigate");
        headers.add("Sec-Fetch-Site", "none");
        headers.add("Sec-Fetch-User", "?1");
        headers.add("Upgrade-Insecure-Requests", "1");
        headers.add("User-Agent", getUserAgent(USER_AGENT));
        return headers.build();
    }

}
