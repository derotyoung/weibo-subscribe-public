package com.derotyoung.weibosubscribe;

import com.alibaba.fastjson2.JSON;
import com.derotyoung.properties.WeiboSubscribeProperties;
import com.derotyoung.service.SearchService;
import com.derotyoung.util.OkHttpClientUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneId;

@SpringBootTest
public class WeiboSubscribeApplicationTests {

    @Autowired
    private SearchService searchService;

    @Autowired
    private WeiboSubscribeProperties weiboSubscribeProperties;

    @Test
    void testSearch() {
        searchService.run();
    }

    // @Test
    void testWeiboId() {
        searchService.testWeiboId();
    }

    // @Test
    void testProxy() {
        searchService.testProxy();
    }

    // @Test
    void testBot() {
        // Create your bot passing the token received from @BotFather
        String botToken = weiboSubscribeProperties.getTelegramBotToken();
        String chatId = weiboSubscribeProperties.getTelegramChatId();
        String proxy = weiboSubscribeProperties.getProxy();

        OkHttpClient client = OkHttpClientUtil.client(proxy);

        TelegramBot bot = new TelegramBot.Builder(botToken).okHttpClient(client).build();

        String text = "Hello Telegram Channel!";

        SendMessage sendMessage = new SendMessage(chatId, text);
        sendMessage.parseMode(ParseMode.Markdown);
        sendMessage.disableWebPagePreview(true);
        SendResponse response = bot.execute(sendMessage);

        System.out.println("response = " + JSON.toJSONString(response));
    }

    @Test
    void testAny() {

        LocalDateTime l1 = LocalDateTime.now(ZoneId.of("UTC+06"));
        LocalDateTime l2 = LocalDateTime.now(ZoneId.of("UTC+08"));
        System.out.println("l1 = " + l1);
        System.out.println("l2 = " + l2);
    }

}
