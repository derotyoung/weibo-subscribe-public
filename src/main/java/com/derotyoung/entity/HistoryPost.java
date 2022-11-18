package com.derotyoung.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "history_post")
public class HistoryPost {

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
