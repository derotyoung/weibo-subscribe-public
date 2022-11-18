package com.derotyoung.util;

import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

public class FileUtil {

    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
     * 得到文件字节
     *
     * @param url 网络文件URL地址
     */
    public static byte[] getBytes(String url) {
        if (StringUtils.hasLength(url)) {
            try (Response response = OkHttpClientUtil.request(url)) {
                if (response.code() != 200) {
                    return null;
                }
                return Objects.requireNonNull(response.body()).bytes();
            } catch (IOException e) {
                logger.error("文件下载异常,url={}", url, e);
            }
        }

        return null;
    }
}
