package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

import java.time.LocalDate;
import java.util.Set;

public final class InputValidator {
    public static final int AMOUNT_MIN = 1;
    public static final int AMOUNT_MAX = 100_000_000;
    private static final Set<String> REPEATS = Set.of("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY");

    private InputValidator() {
    }

    public static void requireCategory(Integer categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new BizException(ErrorCode.E102, "카테고리를 선택해야 합니다");
        }
    }

    public static int requireAmount(Integer amount, boolean present) {
        if (!present || amount == null) {
            throw new BizException(ErrorCode.E102, "금액을 입력해야 합니다");
        }
        if (amount < AMOUNT_MIN || amount > AMOUNT_MAX) {
            throw new BizException(ErrorCode.E103, "금액은 1~100,000,000원이어야 합니다");
        }
        return amount;
    }

    public static String repeatOrNone(String repeatCycle, boolean present) {
        if (!present || repeatCycle == null || repeatCycle.isBlank()) {
            return "NONE";
        }
        String value = repeatCycle.trim();
        if (!REPEATS.contains(value)) {
            throw new BizException(ErrorCode.E103, "반복 주기가 올바르지 않습니다");
        }
        return value;
    }

    public static String memoOrNull(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            throw new BizException(ErrorCode.E103, "메모는 100자 이하여야 합니다");
        }
        return trimmed;
    }

    /**
     * 지출이면 고정/유동이 필수이고, 수입이면 서버가 fixedType 을 비운다.
     * @return 저장할 fixedType. 수입이면 null.
     */
    public static String fixedTypeFor(String categoryType, String fixedType, boolean present) {
        if ("INCOME".equals(categoryType)) {
            return null;
        }
        if (!"EXPENSE".equals(categoryType)) {
            throw new BizException(ErrorCode.E103, "카테고리 구분이 올바르지 않습니다");
        }
        if (!present || fixedType == null || fixedType.isBlank()) {
            throw new BizException(ErrorCode.E102, "지출 유형(고정/유동) 선택 필요");
        }
        String value = fixedType.trim();
        if (!"FIXED".equals(value) && !"VARIABLE".equals(value)) {
            throw new BizException(ErrorCode.E103, "지출 유형은 FIXED 또는 VARIABLE 이어야 합니다");
        }
        return value;
    }

    public static LocalDate requireDate(String text, boolean present) {
        return DateUtil.parseDate(text, present);
    }
}
