package com.derotyoung.enums;

public enum MediaTypeEnum {

    VIDEO("video", "视频"),
    PHOTO("photo", "图");

    MediaTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    private final String value;

    private final String desc;

    public String value() {
        return value;
    }

    public static String desc(String value) {
        for (MediaTypeEnum mediaTypeEnum : MediaTypeEnum.values()) {
            if (mediaTypeEnum.value.equals(value)) return mediaTypeEnum.desc;
        }

        return null;
    }
}
