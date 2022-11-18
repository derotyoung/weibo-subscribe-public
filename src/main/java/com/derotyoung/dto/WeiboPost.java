package com.derotyoung.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WeiboPost {

    public WeiboPost() {
    }

    public WeiboPost(String text, String nickname) {
        this.text = text;
        this.nickname = nickname;
    }

    /**
     * 文章ID
     */
    private String id;

    /**
     * 文章文本内容
     */
    private String text;

    /**
     * 是否置顶
     */
    private Integer topFlag;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 来源
     */
    private String source;

    /**
     * 文章链接
     */
    private String link;

    /**
     * 媒体
     */
    private List<Media> mediaList;

    /**
     * 创建于
     */
    private LocalDateTime createdAt;

    /**
     * 编辑于
     */
    private LocalDateTime editAt;
}
