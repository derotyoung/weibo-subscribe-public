package com.derotyoung.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.derotyoung.entity.UserSubscribe;
import com.derotyoung.mapper.UserSubscribeMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserSubscribeRepository extends ServiceImpl<UserSubscribeMapper, UserSubscribe> {

    public List<String> getAllUserId(final boolean opened) {
        List<UserSubscribe> list = list();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        return list.stream().filter(i -> {
                    if (opened) {
                        return Integer.valueOf(1).equals(i.getOpenFlag());
                    }
                    return true;
                }).map(UserSubscribe::getUserId)
                .collect(Collectors.toList());
    }

}
