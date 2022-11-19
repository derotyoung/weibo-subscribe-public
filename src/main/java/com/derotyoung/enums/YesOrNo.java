package com.derotyoung.enums;

public enum YesOrNo {

    NO(0),

    YES(1);

    private final Integer value;

    YesOrNo(Integer value) {
        this.value = value;
    }

    public Integer value() {
        return this.value;
    }

}
