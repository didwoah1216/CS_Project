package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidatorTest {

    @Test
    void amountBoundaries() {
        assertEquals(1, InputValidator.requireAmount(1, true));
        assertEquals(100_000_000, InputValidator.requireAmount(100_000_000, true));
        assertEquals(ErrorCode.E103, assertThrows(BizException.class,
                () -> InputValidator.requireAmount(0, true)).getCode());
        assertEquals(ErrorCode.E103, assertThrows(BizException.class,
                () -> InputValidator.requireAmount(100_000_001, true)).getCode());
        assertEquals(ErrorCode.E102, assertThrows(BizException.class,
                () -> InputValidator.requireAmount(null, false)).getCode());
    }

    @Test
    void impossibleDateAndMissingFixedType() {
        BizException date = assertThrows(BizException.class,
                () -> InputValidator.requireDate("2026-02-30", true));
        assertEquals(ErrorCode.E103, date.getCode());
        assertEquals("존재하지 않는 날짜입니다", date.getMessage());

        BizException fixed = assertThrows(BizException.class,
                () -> InputValidator.fixedTypeFor("EXPENSE", null, false));
        assertEquals(ErrorCode.E102, fixed.getCode());
        assertEquals("지출 유형(고정/유동) 선택 필요", fixed.getMessage());
    }

    @Test
    void incomeClearsFixedTypeAndMemoLimit() {
        assertNull(InputValidator.fixedTypeFor("INCOME", "FIXED", true));
        assertEquals("VARIABLE", InputValidator.fixedTypeFor("EXPENSE", "VARIABLE", true));
        assertEquals(ErrorCode.E103, assertThrows(BizException.class,
                () -> InputValidator.memoOrNull("가".repeat(101))).getCode());
        assertNull(InputValidator.memoOrNull("  "));
    }

    @Test
    void categoryRequired() {
        assertEquals(ErrorCode.E102, assertThrows(BizException.class,
                () -> InputValidator.requireCategory(null)).getCode());
    }
}
