package com.derotyoung.repository;

import com.derotyoung.entity.HistoryPost;
import com.derotyoung.mapper.HistoryPostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class HistoryPostRepository {

    @Autowired
    private HistoryPostMapper historyPostMapper;

    public List<HistoryPost> list() {
        return Optional.of(historyPostMapper.findAll()).orElse(Collections.emptyList());
    }

    public void saveBatch(List<HistoryPost> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        historyPostMapper.saveAll(list);
    }

    public void removeBatchByIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }
        historyPostMapper.deleteAllByIdInBatch(idList);
    }

    public Map<String, List<HistoryPost>> getPostMapByUserId(String userId) {
        List<HistoryPost> list = historyPostMapper.findAllByUserId(userId);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }

        return list.stream().collect(Collectors.groupingBy(HistoryPost::getPostId));
    }

}
