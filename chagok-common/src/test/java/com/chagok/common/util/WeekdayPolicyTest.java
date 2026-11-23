package com.chagok.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekdayPolicyTest {

    @Test
    void weekendIsFridayThroughSundayAndThresholdIsFifty() {
        assertTrue(WeekdayPolicy.weekend(6));
        assertTrue(WeekdayPolicy.weekend(7));
        assertTrue(WeekdayPolicy.weekend(1));
        assertFalse(WeekdayPolicy.weekend(5));
        assertFalse(WeekdayPolicy.concentrated(49.9));
        assertTrue(WeekdayPolicy.concentrated(50.0));
        assertEquals("토", WeekdayPolicy.name(7));
        assertTrue(WeekdayPolicy.message(1_527_300, "토", 21.5, 49.9).contains("기준(50%) 미만"));
        assertEquals("이 기간의 유동지출이 없습니다.", WeekdayPolicy.message(0, "", 0, 0));
    }
}
