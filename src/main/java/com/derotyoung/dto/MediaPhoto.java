package com.derotyoung.dto;

import com.derotyoung.enums.MediaTypeEnum;

public class MediaPhoto extends Media {

    public MediaPhoto(String media) {
        super(MediaTypeEnum.PHOTO.getValue(), media);
    }

    public MediaPhoto(String media, String thumb) {
        this(media);
        this.setThumb(thumb);
    }
}
