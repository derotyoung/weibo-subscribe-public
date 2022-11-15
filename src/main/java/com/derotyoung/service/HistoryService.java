package com.derotyoung.service;

import com.derotyoung.dto.WeiboPost;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.properties.WeiboSubscribeProperties;
import com.derotyoung.repository.HistoryPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final static Logger logger = LoggerFactory.getLogger(HistoryService.class);

    @Autowired
    private HistoryPostRepository historyPostRepository;

    @Autowired
    private WeiboSubscribeProperties weiboSubscribeProperties;

    /**
     * 保存发送记录
     */
    public void saveHistory(List<WeiboPost> weiboPostList) {
        if (CollectionUtils.isEmpty(weiboPostList)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC+08"));
        List<HistoryPost> postList = new LinkedList<>();
        for (WeiboPost weiboPost : weiboPostList) {
            HistoryPost historyPost = new HistoryPost();
            historyPost.setUserId(weiboPost.getUserId());
            historyPost.setNickname(weiboPost.getNickname());
            historyPost.setPostId(weiboPost.getId());
            historyPost.setTopFlag(weiboPost.getTopFlag());
            historyPost.setCreatedAt(weiboPost.getCreatedAt());
            historyPost.setEditAt(weiboPost.getEditAt());
            historyPost.setCreateTime(now);
            postList.add(historyPost);
        }
        historyPostRepository.saveBatch(postList);
    }

    /**
     * 清理历史记录
     */
    public void clearHistory() {
        // 每个用户最多保留100条
        List<HistoryPost> list = historyPostRepository.list();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        final int limit = 100;
        List<Long> deletedIdList = new ArrayList<>();
        Map<String, List<HistoryPost>> userPostMap = list.stream().collect(Collectors.groupingBy(HistoryPost::getUserId));
        userPostMap.forEach((k, v) -> {
            if (v.size() > limit) {
                List<Long> idList = v.stream().filter(i -> Integer.valueOf(0).equals(i.getTopFlag()))
                        .sorted(Comparator.comparing(HistoryPost::getId).reversed())
                        .skip(limit)
                        .map(HistoryPost::getId).toList();
                deletedIdList.addAll(idList);
            }
        });
        if (!deletedIdList.isEmpty()) {
            historyPostRepository.removeBatchByIds(deletedIdList);
            logger.info("already clear {} history records", deletedIdList);
        }
    }

}

