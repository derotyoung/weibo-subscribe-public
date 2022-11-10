package com.derotyoung.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "weibo.subscribe")
public class WeiboSubscribeProperties {

    /**
     * 在Telegram创建的频道的ID
     */
    private String telegramChatId;

    /**
     * 在Telegram申请的bot token
     */
    private String telegramBotToken;

    /**
     * 定时循环执行，单位：分钟，默认3分钟
     */
    private long cyclePeriod = 3;

    /**
     * 微博数字ID，多个微博ID用英文逗号隔开
     * 注意：请尽量不要设置过多的微博ID，避免被新浪微博识别为恶意爬虫，被拉黑IP
     * 欢迎在大家在issues中交流大家的 订阅数量 以及 访问频次
     */
    private List<String> userIds;

    /**
     * 在中国大陆境内给Telegram发送消息需要科学上网
     * 本脚本支持http代理，请正确配置ip以及端口
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

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
}
