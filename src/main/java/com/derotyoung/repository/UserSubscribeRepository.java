package com.derotyoung.repository;

import com.derotyoung.entity.UserSubscribe;
import com.derotyoung.mapper.UserSubscribeMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserSubscribeRepository {

    private final UserSubscribeMapper userSubscribeMapper;

    public UserSubscribeRepository(UserSubscribeMapper userSubscribeMapper) {
        this.userSubscribeMapper = userSubscribeMapper;
    }

    public List<String> getAllUserId(final boolean opened) {
        List<UserSubscribe> list = userSubscribeMapper.findAll();
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
