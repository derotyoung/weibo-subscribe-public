package com.derotyoung.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.derotyoung.dto.WeiboPost;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.enums.YesOrNo;
import com.derotyoung.repository.HistoryPostRepository;
import com.derotyoung.util.PostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final static Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private final HistoryPostRepository historyPostRepository;

    public HistoryService(HistoryPostRepository historyPostRepository) {
        this.historyPostRepository = historyPostRepository;
    }

    public void run() {
        clearHistory();
        checkPostIsTopAndUpdate();
    }

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
            historyPost.setMessageId(weiboPost.getMessageId());
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
    private void clearHistory() {
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
                List<Long> idList = v.stream().filter(i -> YesOrNo.NO.value().equals(i.getTopFlag()))
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

    /**
     * 检查置顶的博文是否仍然置顶
     */
    private void checkPostIsTopAndUpdate() {
        List<HistoryPost> list = historyPostRepository.list(YesOrNo.YES.value());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        List<HistoryPost> notTopPostList = new ArrayList<>();
        list.stream().collect(Collectors.groupingBy(HistoryPost::getUserId)).forEach((userId, v) -> {
            // 最新置顶博文id
            List<String> latestIdList = new ArrayList<>();
            JSONArray cards = PostUtil.getUserIndex(userId);
            if (!CollectionUtils.isEmpty(cards)) {
                for (int i = 0; i < cards.size(); i++) {
                    if (i >= 3) {
                        break;
                    }
                    JSONObject card = cards.getJSONObject(i);
                    JSONObject mblog = card.getJSONObject("mblog");
                    boolean topPost = PostUtil.isTopPost(mblog);
                    if (topPost) {
                        latestIdList.add(mblog.getString("id"));
                    }
                }
            }
            // 不在最新的则不再置顶
            for (HistoryPost oldTopPost : v) {
                if (!latestIdList.contains(oldTopPost.getPostId())) {
                    oldTopPost.setTopFlag(YesOrNo.NO.value());
                    notTopPostList.add(oldTopPost);
                }
            }
        });

        if (!notTopPostList.isEmpty()) {
            historyPostRepository.saveBatch(notTopPostList);
        }
    }

}

