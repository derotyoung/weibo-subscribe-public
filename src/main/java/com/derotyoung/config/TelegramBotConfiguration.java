package com.derotyoung.config;

import com.derotyoung.properties.WeiboSubscribeProperties;
import com.derotyoung.util.OkHttpClientUtil;
import com.pengrad.telegrambot.TelegramBot;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Autowired
    private WeiboSubscribeProperties weiboSubscribeProperties;

    @Bean
    public TelegramBot telegramBot() {
        String proxy = weiboSubscribeProperties.getProxy();
        String botToken = weiboSubscribeProperties.getTelegramBotToken();
        OkHttpClient client = OkHttpClientUtil.client(proxy);
        return new TelegramBot.Builder(botToken).okHttpClient(client).build();
    }

}
