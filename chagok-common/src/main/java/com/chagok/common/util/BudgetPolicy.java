package com.chagok.common.util;

/**
 * FR-10 예산 사용률과 상태.
 * 사용률 = 사용액 / 예산 × 100 (소수 첫째 자리).
 * NORMAL 은 알림 기준 미만, ALERT 는 기준 이상 100% 미만, OVER 는 100% 이상.
 */
public final class BudgetPolicy {
    public static final String NORMAL = "NORMAL";
    public static final String ALERT = "ALERT";
    public static final String OVER = "OVER";

    private BudgetPolicy() {
    }

    public static double usageRate(long used, int amount) {
        if (amount <= 0) {
            return 0;
        }
        return WarningPolicy.round1(used * 100.0 / amount);
    }

    public static String status(double usageRate, int alertRatio) {
        if (usageRate >= 100.0) {
            return OVER;
        }
        if (usageRate >= alertRatio) {
            return ALERT;
        }
        return NORMAL;
    }

    /** 점수(FR-14)의 예산 준수. 사용액이 예산 이하이면 지킨 것으로 본다. */
    public static boolean kept(long used, int amount) {
        return used <= amount;
    }
}
