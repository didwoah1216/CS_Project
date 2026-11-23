package com.chagok.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-11 반복 내역이 대상 월에 떨어질 날짜.
 * 원본 날짜 자체는 포함하지 않고, 그 다음 주기부터 만든다.
 * 매월·매년은 원본의 일(day)을 쓰고, 그 달에 없는 날이면 말일로 당긴다.
 */
public final class RecurringPolicy {
    private RecurringPolicy() {
    }

    public static List<LocalDate> occurrences(LocalDate origin, String cycle, YearMonth month) {
        if (origin == null || month == null || cycle == null || "NONE".equals(cycle)) {
            return List.of();
        }
        return switch (cycle) {
            case "DAILY" -> daily(origin, month);
            case "WEEKLY" -> step(origin, month, 7);
            case "MONTHLY" -> anchored(origin, month, true);
            case "YEARLY" -> anchored(origin, month, false);
            default -> List.of();
        };
    }

    private static List<LocalDate> daily(LocalDate origin, YearMonth month) {
        LocalDate cursor = origin.plusDays(1);
        LocalDate start = month.atDay(1);
        if (cursor.isBefore(start)) {
            cursor = start;
        }
        List<LocalDate> dates = new ArrayList<>();
        LocalDate end = month.atEndOfMonth();
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private static List<LocalDate> step(LocalDate origin, YearMonth month, int days) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = origin.plusDays(days);
        LocalDate end = month.atEndOfMonth();
        while (!cursor.isAfter(end)) {
            if (!cursor.isBefore(month.atDay(1))) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(days);
        }
        return dates;
    }

    private static List<LocalDate> anchored(LocalDate origin, YearMonth month, boolean everyMonth) {
        if (!month.isAfter(YearMonth.from(origin))) {
            return List.of();
        }
        if (!everyMonth && (month.getMonthValue() != origin.getMonthValue() || month.getYear() <= origin.getYear())) {
            return List.of();
        }
        int day = Math.min(origin.getDayOfMonth(), month.lengthOfMonth());
        return List.of(month.atDay(day));
    }
}
