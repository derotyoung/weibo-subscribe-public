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

    private static int getRandomNumberInRange(int min, int max) {
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

    public static String getRequestUrl(String url, Map<String, String> paramsMap) {
        StringBuilder reqUrl = new StringBuilder(url);
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
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json; charset=UTF-8");
        headers.add("User-Agent", getUserAgent(USER_AGENT));
        headers.add("Accept", "image/avif,image/webp,*/*");

        Request request = new Request.Builder()
                .url(url)
                .headers(headers.build())
                .build();

        OkHttpClient client = client(null);
        Response response = client.newCall(request).execute();

        return Objects.requireNonNull(response.body()).string();
    }

    public static String requestGet(String url, Map<String, String> paramsMap) throws IOException {
        return requestGet(getRequestUrl(url, paramsMap));
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

}
