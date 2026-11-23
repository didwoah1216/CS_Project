package com.chagok.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetPolicyTest {

    @Test
    void octoberRatesMatchTheReport() {
        assertEquals(87.0, BudgetPolicy.usageRate(304_500, 350_000));
        assertEquals(97.7, BudgetPolicy.usageRate(58_600, 60_000));
        assertEquals(82.8, BudgetPolicy.usageRate(49_700, 60_000));
        assertEquals(145.5, BudgetPolicy.usageRate(145_500, 100_000));
        assertEquals(37.3, BudgetPolicy.usageRate(22_400, 60_000));
        assertEquals(186.9, BudgetPolicy.usageRate(149_500, 80_000));
    }

    @Test
    void statusUsesAlertThresholdAndHundred() {
        assertEquals(BudgetPolicy.NORMAL, BudgetPolicy.status(37.3, 80));
        assertEquals(BudgetPolicy.NORMAL, BudgetPolicy.status(79.9, 80));
        assertEquals(BudgetPolicy.ALERT, BudgetPolicy.status(80.0, 80));
        assertEquals(BudgetPolicy.ALERT, BudgetPolicy.status(87.0, 80));
        assertEquals(BudgetPolicy.ALERT, BudgetPolicy.status(99.9, 80));
        assertEquals(BudgetPolicy.OVER, BudgetPolicy.status(100.0, 80));
        assertEquals(BudgetPolicy.OVER, BudgetPolicy.status(145.5, 80));
        assertEquals(BudgetPolicy.NORMAL, BudgetPolicy.status(87.0, 90));
    }

    @Test
    void exactBudgetStillCountsAsKept() {
        assertTrue(BudgetPolicy.kept(1000, 1000));
        assertFalse(BudgetPolicy.kept(1001, 1000));
    }
}
