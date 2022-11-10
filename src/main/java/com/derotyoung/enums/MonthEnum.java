package com.derotyoung.enums;

public enum MonthEnum {

    JANUARY("Jan", 1),
    FEBRUARY("Feb", 2),
    MARCH("Mar", 3),
    APRIL("Apr", 4),
    MAY("May", 5),
    JUNE("Jun", 6),
    JULY("Jul", 7),
    AUGUST("Aug", 8),
    SEPTEMBER("Sep", 9),
    OCTOBER("Oct", 10),
    NOVEMBER("Nov", 11),
    DECEMBER("Dec", 12);

    MonthEnum(String shortName, int value) {
        this.shortName = shortName;
        this.value = value;
    }

    private final String shortName;

    private final int value;

    public static int valueFrom(String shortName) {
        for (MonthEnum value : MonthEnum.values()) {
            if (shortName.equals(value.shortName)) {
                return value.value;
            }
        }
        return -1;
    }

}
