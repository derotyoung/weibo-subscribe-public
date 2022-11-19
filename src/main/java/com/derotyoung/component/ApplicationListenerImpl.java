package com.derotyoung.component;

import com.derotyoung.WeiboSubscribeApplication;
import com.derotyoung.config.WeiboSubscribe;
import com.derotyoung.service.HistoryService;
import com.derotyoung.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ApplicationListenerImpl implements ApplicationListener<ApplicationStartedEvent> {

    private final static Logger logger = LoggerFactory.getLogger(WeiboSubscribeApplication.class);

    private final WeiboSubscribe weiboSubscribe;

    private final PostService postService;

    private final HistoryService historyService;

    public ApplicationListenerImpl(WeiboSubscribe weiboSubscribe,
                                   PostService postService,
                                   HistoryService historyService) {
        this.weiboSubscribe = weiboSubscribe;
        this.postService = postService;
        this.historyService = historyService;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        logger.info("微博订阅启动-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
        Runnable task = () -> {
            // logger.info("开始执行查询任务-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
            // 01:00 - 06:00 不执行，强制使用东8区时间
            LocalTime localTime = LocalTime.now(ZoneId.of("UTC+08"));
            if (localTime.isAfter(LocalTime.of(1, 0))
                    && localTime.isBefore(LocalTime.of(6, 0))) {
                return;
            }
            postService.run();
        };
        // 启动后延时 10s 后，按 weibo.subscribe.cyclePeriod 秒 的周期执行任务
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(2);
        threadPool.scheduleWithFixedDelay(task, 10, weiboSubscribe.getCyclePeriod(), TimeUnit.SECONDS);

        Runnable clearTask = () -> {
            logger.info("开始执行历史记录处理任务-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
            historyService.run();
        };
        // 每过24小时清理一次历史记录
        threadPool.scheduleWithFixedDelay(clearTask, 1, 24, TimeUnit.HOURS);
    }

}


