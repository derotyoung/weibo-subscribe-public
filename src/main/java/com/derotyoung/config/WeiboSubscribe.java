package com.derotyoung.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@ConfigurationProperties(prefix = "weibo.subscribe")
public class WeiboSubscribe {

    private static final long MIN_CYCLE_PERIOD = SECONDS.toSeconds(10);

    public WeiboSubscribe() {
        validate();
    }

    public void validate() {
        if (cyclePeriod < MIN_CYCLE_PERIOD) {
            cyclePeriod = MIN_CYCLE_PERIOD;
        }
    }

    /**
     * 在Telegram创建的频道的ID
     */
    private String telegramChatId;

    /**
     * 在Telegram申请的bot token
     */
    private String telegramBotToken;

    /**
     * 定时循环执行，单位：秒，默认30s
     */
    private long cyclePeriod = 30;

    /**
     * 在中国大陆境内给Telegram发送消息需要科学上网
     * 本脚本只支持http代理，请正确配置ip以及端口
     * 如果在非中国大陆境内使用，请留空
     * http代理格式为:<a href="http://127.0.0.1:7890">...</a>
     */
    private String proxy;

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public long getCyclePeriod() {
        return cyclePeriod;
    }

    public void setCyclePeriod(long cyclePeriod) {
        this.cyclePeriod = cyclePeriod;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
}
