package com.chagok.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringPolicyTest {

    @Test
    void monthlyUsesTheSameDayInALaterMonth() {
        List<LocalDate> dates = RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 5), "MONTHLY", YearMonth.of(2026, 11));
        assertEquals(List.of(LocalDate.of(2026, 11, 5)), dates);
        assertTrue(RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 5), "MONTHLY", YearMonth.of(2026, 9)).isEmpty());
    }

    @Test
    void monthlyClampsToTheLastDay() {
        assertEquals(List.of(LocalDate.of(2026, 2, 28)), RecurringPolicy.occurrences(
                LocalDate.of(2026, 1, 31), "MONTHLY", YearMonth.of(2026, 2)));
        assertEquals(List.of(LocalDate.of(2024, 2, 29)), RecurringPolicy.occurrences(
                LocalDate.of(2024, 1, 31), "MONTHLY", YearMonth.of(2024, 2)));
    }

    @Test
    void weeklyAndDailyStayInsideTheMonth() {
        assertEquals(List.of(
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 24)), RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 1), "WEEKLY", YearMonth.of(2026, 11)));
        assertEquals(List.of(LocalDate.of(2026, 11, 29), LocalDate.of(2026, 11, 30)),
                RecurringPolicy.occurrences(LocalDate.of(2026, 11, 28), "DAILY", YearMonth.of(2026, 11)));
    }

    @Test
    void yearlyMatchesMonthAndSkipsTheOriginYear() {
        assertEquals(List.of(LocalDate.of(2027, 9, 5)), RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 5), "YEARLY", YearMonth.of(2027, 9)));
        assertTrue(RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 5), "YEARLY", YearMonth.of(2027, 10)).isEmpty());
        assertTrue(RecurringPolicy.occurrences(
                LocalDate.of(2026, 9, 5), "NONE", YearMonth.of(2027, 9)).isEmpty());
    }
}
