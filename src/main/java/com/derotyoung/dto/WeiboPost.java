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

    private String id;

    private String text;

    private String userId;

    private String nickname;

    private String source;

    private List<String> pics;

    private String link;

    private String videoUrl;

    private LocalDateTime createdAt;

    private LocalDateTime editAt;
}
