package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** FR-15 저축 목표. 진행 중 목표는 5개까지, 입금액 0은 허용하지 않는다. */
public final class GoalPolicy {
    public static final int ACTIVE_LIMIT = 5;
    public static final int NAME_MAX = 30;
    public static final int TARGET_MIN = 1_000;
    public static final long TARGET_MAX = 1_000_000_000L;
    public static final int DEPOSIT_ABS_MAX = 100_000_000;
    public static final int MEMO_MAX = 50;

    private GoalPolicy() {
    }

    public static String name(String raw, boolean present) {
        if (!present || raw == null || raw.isBlank()) {
            throw new BizException(ErrorCode.E102, "목표 이름을 입력해야 합니다");
        }
        String name = raw.trim();
        if (name.length() > NAME_MAX) {
            throw new BizException(ErrorCode.E103, "목표 이름은 30자 이하여야 합니다");
        }
        return name;
    }

    public static int targetAmount(Integer amount, boolean present) {
        if (!present || amount == null) {
            throw new BizException(ErrorCode.E102, "목표 금액을 입력해야 합니다");
        }
        if (amount < TARGET_MIN || amount > TARGET_MAX) {
            throw new BizException(ErrorCode.E103, "목표 금액은 1,000~1,000,000,000원이어야 합니다");
        }
        return amount;
    }

    public static void requireTargetNotBeforeStart(LocalDate start, LocalDate target) {
        if (target.isBefore(start)) {
            throw new BizException(ErrorCode.E304, "목표일은 시작일 이후여야 합니다");
        }
    }

    public static int depositAmount(Integer amount, boolean present) {
        if (!present || amount == null) {
            throw new BizException(ErrorCode.E102, "입금액을 입력해야 합니다");
        }
        if (amount == 0 || Math.abs((long) amount) > DEPOSIT_ABS_MAX) {
            throw new BizException(ErrorCode.E103, "입금액은 0이 아닌 -100,000,000~100,000,000원이어야 합니다");
        }
        return amount;
    }

    public static String memo(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MEMO_MAX) {
            throw new BizException(ErrorCode.E103, "입금 메모는 50자 이하여야 합니다");
        }
        return trimmed;
    }

    public static double progress(int saved, int target) {
        if (target <= 0) {
            return 0;
        }
        return WarningPolicy.round1(saved * 100.0 / target);
    }

    /**
     * 남은 금액을 이번 달부터 목표 월까지(양 끝 포함) 나눈 값. 이미 모았거나 목표일이 지났으면 남은 금액 전체.
     */
    public static long monthlyNeed(int saved, int target, LocalDate today, LocalDate targetDate) {
        long remaining = Math.max(0L, (long) target - saved);
        if (remaining == 0 || targetDate.isBefore(today)) {
            return remaining;
        }
        long months = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1)) + 1;
        if (months < 1) {
            months = 1;
        }
        return (long) Math.ceil(remaining / (double) months);
    }

    public static int dDay(LocalDate today, LocalDate targetDate) {
        return (int) ChronoUnit.DAYS.between(today, targetDate);
    }

    public static String nextStatus(String current, int saved, int target) {
        if ("CANCELED".equals(current)) {
            return "CANCELED";
        }
        return saved >= target ? "ACHIEVED" : "ACTIVE";
    }
}
