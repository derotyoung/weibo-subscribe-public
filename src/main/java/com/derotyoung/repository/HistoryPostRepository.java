package com.derotyoung.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.derotyoung.entity.HistoryPost;
import com.derotyoung.mapper.HistoryPostMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class HistoryPostRepository extends ServiceImpl<HistoryPostMapper, HistoryPost> {

    public Map<String, List<HistoryPost>> getPostMapByUserId(String userId) {
        LambdaQueryWrapper<HistoryPost> wrapper = Wrappers.lambdaQuery(HistoryPost.class)
                .eq(HistoryPost::getUserId, userId)
                .last("limit 300");
        wrapper.orderByDesc(HistoryPost::getId);
        List<HistoryPost> list = list(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }

        return list.stream().collect(Collectors.groupingBy(HistoryPost::getPostId));
    }
}
