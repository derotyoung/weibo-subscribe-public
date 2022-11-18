package com.derotyoung.dto;

public abstract class Media {

    private final String type;

    private final String media;

    private String thumb;

    public Media(String type, String media) {
        this.type = type;
        this.media = media;
    }

    public String getType() {
        return type;
    }

    public String getMedia() {
        return media;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }
}
