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

    private String userId;

    private String nickname;

    private Integer openFlag;

    private LocalDateTime createTime;
}
