package com.derotyoung.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_subscribe")
public class UserSubscribe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 是否开启订阅
     */
    private Integer openFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
