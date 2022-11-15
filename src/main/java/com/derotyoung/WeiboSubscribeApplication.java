package com.derotyoung;

import com.derotyoung.properties.WeiboSubscribeProperties;
import com.derotyoung.service.HistoryService;
import com.derotyoung.service.SearchService;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@MapperScan("com.derotyoung.mapper")
@ConfigurationPropertiesScan("com.derotyoung.properties")
@SpringBootApplication
public class WeiboSubscribeApplication {

    private final static Logger logger = LoggerFactory.getLogger(WeiboSubscribeApplication.class);

    @Autowired
    private WeiboSubscribeProperties weiboSubscribeProperties;

    @Autowired
    private SearchService searchService;

    @Autowired
    private HistoryService historyService;

    public static void main(String[] args) {
        SpringApplication.run(WeiboSubscribeApplication.class, args);
    }

    @PostConstruct
    public void execute() {
        logger.info("微博订阅启动-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
        Runnable task = () -> {
            logger.info("开始执行查询任务-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
            // 01:00 - 06:00 不执行，强制使用东8区时间
            LocalTime localTime = LocalTime.now(ZoneId.of("UTC+08"));
            if (localTime.isAfter(LocalTime.of(1, 0))
                    && localTime.isBefore(LocalTime.of(6, 0))) {
                return;
            }
            searchService.run();
        };
        // 启动后延时 1 分钟后，按 weibo.subscribe.cyclePeriod分钟 的周期执行任务
        ScheduledExecutorService timerPool = Executors.newScheduledThreadPool(2);
        timerPool.scheduleAtFixedRate(task, 15, (weiboSubscribeProperties.getCyclePeriod() * 60), TimeUnit.SECONDS);

        Runnable clearTask = () -> {
            logger.info("开始执行清理任务-{}", LocalDateTime.now(ZoneId.of("UTC+08")));
            historyService.clearHistory();
        };
        // 每过24小时清理一次历史记录
        timerPool.scheduleWithFixedDelay(clearTask, 1, 24, TimeUnit.HOURS);
    }

}
