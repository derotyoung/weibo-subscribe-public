package com.derotyoung.config;

import com.derotyoung.util.OkHttpClientUtil;
import com.pengrad.telegrambot.TelegramBot;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    public TelegramBot telegramBot(WeiboSubscribe weiboSubscribe) {
        String proxy = weiboSubscribe.getProxy();
        String botToken = weiboSubscribe.getTelegramBotToken();
        OkHttpClient client = OkHttpClientUtil.client(proxy);
        return new TelegramBot.Builder(botToken).okHttpClient(client).build();
    }

}
