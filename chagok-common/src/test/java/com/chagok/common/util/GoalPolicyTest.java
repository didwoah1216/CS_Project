package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoalPolicyTest {

    @Test
    void progressAndMonthlyNeed() {
        assertEquals(24.0, GoalPolicy.progress(120_000, 500_000));
        assertEquals(190_000, GoalPolicy.monthlyNeed(120_000, 500_000,
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 31)));
        assertEquals(38, GoalPolicy.dDay(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 31)));
        assertEquals(0, GoalPolicy.monthlyNeed(500_000, 500_000,
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 31)));
        assertEquals(100_000, GoalPolicy.monthlyNeed(0, 100_000,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void rulesRejectBlankNameZeroDepositAndPastTarget() {
        assertEquals("E102", assertThrows(BizException.class, () -> GoalPolicy.name("  ", true)).getCode().getCode());
        assertEquals("E103", assertThrows(BizException.class, () -> GoalPolicy.targetAmount(999, true)).getCode().getCode());
        assertEquals("E103", assertThrows(BizException.class, () -> GoalPolicy.depositAmount(0, true)).getCode().getCode());
        BizException dates = assertThrows(BizException.class, () -> GoalPolicy.requireTargetNotBeforeStart(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)));
        assertEquals("E304", dates.getCode().getCode());
        assertEquals("ACHIEVED", GoalPolicy.nextStatus("ACTIVE", 500_000, 500_000));
        assertEquals("ACTIVE", GoalPolicy.nextStatus("ACHIEVED", 100, 500_000));
        assertEquals("CANCELED", GoalPolicy.nextStatus("CANCELED", 500_000, 500_000));
        assertEquals(GoalPolicy.ACTIVE_LIMIT, 5);
    }
}
