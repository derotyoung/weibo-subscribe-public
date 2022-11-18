package com.derotyoung.mapper;

import com.derotyoung.entity.UserSubscribe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscribeMapper extends JpaRepository<UserSubscribe, Long> {

}
