package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComparePolicyTest {

    @Test
    void sampleUnderThreeIsE401() {
        BizException ex = assertThrows(BizException.class, () -> ComparePolicy.requireSample(2));
        assertEquals(ErrorCode.E401, ex.getCode());
        assertEquals("비교할 같은 연령대 사용자가 부족함", ex.getMessage());
        assertThrows(BizException.class, () -> ComparePolicy.requireSample(0));
        assertThrows(BizException.class, () -> ComparePolicy.requireSample(1));
    }

    @Test
    void sampleOfThreeIsAllowed() {
        assertDoesNotThrow(() -> ComparePolicy.requireSample(3));
    }

    @Test
    void ageBandOf23IsTwenties() {
        AgeGroup group = AgeGroup.of(23);
        assertEquals(20, group.from());
        assertEquals(29, group.to());
    }
}
