package com.derotyoung.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserSubscribe {

    @TableId(type = IdType.AUTO)
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
