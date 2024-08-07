package com.exptech.eew.client.utils;

public class IntensityConversion {

    public static String intensityToNumberString(int level) {
        return switch (level) {
            case 5 -> "5弱";
            case 6 -> "5強";
            case 7 -> "6弱";
            case 8 -> "6強";
            case 9 -> "7";
            default -> String.valueOf(level);
        };
    }
}