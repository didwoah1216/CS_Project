package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchPolicyTest {

    @Test
    void reversedPeriodAndAmountRangeAreRejected() {
        BizException dates = assertThrows(BizException.class, () -> SearchPolicy.requireChronological(
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)));
        assertEquals("E103", dates.getCode().getCode());
        BizException amounts = assertThrows(BizException.class,
                () -> SearchPolicy.requireMinNotAboveMax(5000, 1000));
        assertEquals("E103", amounts.getCode().getCode());
        assertNull(SearchPolicy.keyword("  "));
        assertEquals("%학식%", SearchPolicy.likePattern("학식"));
        assertEquals("%100\\%%", SearchPolicy.likePattern("100%"));
    }

    @Test
    void amountBoundUsesTheTransactionRange() {
        assertEquals(8500, SearchPolicy.amountBound(8500, true, "최소 금액"));
        assertNull(SearchPolicy.amountBound(null, false, "최소 금액"));
        assertEquals("E103", assertThrows(BizException.class,
                () -> SearchPolicy.amountBound(0, true, "최소 금액")).getCode().getCode());
        assertEquals("E103", assertThrows(BizException.class,
                () -> SearchPolicy.keyword("가".repeat(101))).getCode().getCode());
    }
}
