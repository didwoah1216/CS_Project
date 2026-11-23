package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

import java.time.LocalDate;

/** FR-12 검색 조건. 기간이 없으면 전체를 보고, 있으면 시작·종료를 함께 받는다. */
public final class SearchPolicy {
    private SearchPolicy() {
    }

    public static void requireChronological(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BizException(ErrorCode.E103, "시작일이 종료일보다 늦습니다");
        }
    }

    public static Integer amountBound(Integer value, boolean present, String label) {
        if (!present || value == null) {
            return null;
        }
        if (value < InputValidator.AMOUNT_MIN || value > InputValidator.AMOUNT_MAX) {
            throw new BizException(ErrorCode.E103, label + "은 1~100,000,000원이어야 합니다");
        }
        return value;
    }

    public static void requireMinNotAboveMax(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BizException(ErrorCode.E103, "최소 금액이 최대 금액보다 큽니다");
        }
    }

    public static String keyword(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            throw new BizException(ErrorCode.E103, "검색어는 100자 이하여야 합니다");
        }
        return trimmed;
    }

    public static String likePattern(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
