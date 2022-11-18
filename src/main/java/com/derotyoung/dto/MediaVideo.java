package com.derotyoung.dto;

import com.derotyoung.enums.MediaTypeEnum;

public class MediaVideo extends Media {

    public MediaVideo(String media) {
        super(MediaTypeEnum.VIDEO.getValue(), media);
    }

    private Integer width;

    private Integer height;

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
