package com.derotyoung.enums;

public enum MediaTypeEnum {

    VIDEO("video"),
    PHOTO("photo");

    MediaTypeEnum(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
