package com.chagok.common.util;

import java.util.Locale;

/**
 * FR-16 요일 패턴. 주말은 금·토·일이고, 그 비중이 50% 이상이면 주말 소비 집중이다.
 */
public final class WeekdayPolicy {
    public static final double WEEKEND_THRESHOLD = 50.0;
    private static final String[] NAMES = {"", "일", "월", "화", "수", "목", "금", "토"};

    private WeekdayPolicy() {
    }

    public static String name(int mysqlDow) {
        if (mysqlDow < 1 || mysqlDow > 7) {
            return "";
        }
        return NAMES[mysqlDow];
    }

    public static boolean weekend(int mysqlDow) {
        return mysqlDow == 1 || mysqlDow == 6 || mysqlDow == 7;
    }

    public static boolean concentrated(double weekendRatio) {
        return weekendRatio >= WEEKEND_THRESHOLD;
    }

    public static String message(long total, String topName, double topRatio, double weekendRatio) {
        if (total <= 0) {
            return "이 기간의 유동지출이 없습니다.";
        }
        String head = String.format(Locale.US, "%s 유동지출 %.1f%%가 가장 큽니다. ", topName, topRatio);
        if (concentrated(weekendRatio)) {
            return head + String.format(Locale.US, "주말(금~일) 비중 %.1f%%로 기준(50%%) 이상입니다.", weekendRatio);
        }
        return head + String.format(Locale.US, "주말(금~일) 비중 %.1f%%로 기준(50%%) 미만입니다.", weekendRatio);
    }
}
