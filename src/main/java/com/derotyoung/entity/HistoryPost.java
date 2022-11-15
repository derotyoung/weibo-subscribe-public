package com.derotyoung.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HistoryPost {

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
     * 文章ID
     */
    private String postId;

    /**
     * 是否置顶
     */
    private Integer topFlag;

    /**
     * 创建于
     */
    private LocalDateTime createdAt;

    /**
     * 编辑于
     */
    private LocalDateTime editAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
