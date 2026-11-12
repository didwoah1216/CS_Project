package com.chagok.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WarningPolicyTest {

    @Test
    void boundaryLevelsMatchTheTestPlan() {
        assertEquals(WarningPolicy.Level.SAFE, level(49.9));
        assertEquals(WarningPolicy.Level.CAUTION, level(50.0));
        assertEquals(WarningPolicy.Level.CAUTION, level(69.9));
        assertEquals(WarningPolicy.Level.WARNING, level(70.0));
        assertEquals(WarningPolicy.Level.DANGER, level(100.0));
    }

    @Test
    void peerGapUsesStrictGreaterThan() {
        assertEquals(WarningPolicy.Level.CAUTION,
                WarningPolicy.levelOf(60.0, 70, 50.0, 10, false, false));
        assertEquals(WarningPolicy.Level.WARNING,
                WarningPolicy.levelOf(60.1, 70, 50.0, 10, false, false));
    }

    @Test
    void octoberTest01IsWarningWithExpectedMessage() {
        WarningPolicy.Decision decision = WarningPolicy.decide(
                1_282_300, 429_400, 776_300, 70, 10, 55.6, List.of("쇼핑", "술/유흥"));
        assertEquals(WarningPolicy.Level.WARNING, decision.level);
        assertEquals(91.0, decision.ratio);
        assertEquals(852_900, decision.available);
        assertEquals("이번 달 유동지출 비율 91.0%, 기준(70%) 초과. 쇼핑·술/유흥 지출 조절 필요", decision.message);
    }

    @Test
    void raisedThresholdStillWarnsWhenPeerGapIsExceeded() {
        WarningPolicy.Decision decision = WarningPolicy.decide(
                1_282_300, 429_400, 776_300, 95, 10, 55.6, List.of("쇼핑", "술/유흥"));
        assertEquals(WarningPolicy.Level.WARNING, decision.level);
        assertEquals("이번 달 유동지출 비율 91.0%, 또래 평균보다 높습니다. 쇼핑·술/유흥 지출 조절 필요",
                decision.message);
    }

    @Test
    void septemberRatioIsWarning() {
        WarningPolicy.Decision decision = WarningPolicy.decide(
                1_432_700, 429_400, 751_000, 70, 10, 55.6, List.of());
        assertEquals(74.9, decision.ratio);
        assertEquals(WarningPolicy.Level.WARNING, decision.level);
    }

    @Test
    void zeroIncomeIsDangerWithoutRatio() {
        WarningPolicy.Decision decision = WarningPolicy.decide(0, 0, 0, 70, 10, null, List.of());
        assertEquals(WarningPolicy.Level.DANGER, decision.level);
        assertNull(decision.ratio);
        assertEquals("이번 달 수입 내역이 없어 비율 계산 불가", decision.message);
    }

    @Test
    void nonPositiveAvailableIncomeIsDanger() {
        WarningPolicy.Decision decision = WarningPolicy.decide(100_000, 100_000, 10_000, 70, 10, null, List.of());
        assertEquals(WarningPolicy.Level.DANGER, decision.level);
        assertEquals("가용소득 없음(수입 ≤ 고정지출)", decision.message);
    }

    private static WarningPolicy.Level level(double ratio) {
        return WarningPolicy.levelOf(ratio, 70, null, 10, false, false);
    }
}
