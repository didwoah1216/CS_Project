package com.chagok.common.util;

import java.util.Locale;

/**
 * FR-14 소비습관 점수.
 * 유동비율 40, 예산 준수 25, 전월 대비 유동지출 증감 20, 또래 대비 15. 합계 100.
 * 등급은 85 A, 70 B, 55 C, 그 미만 D.
 */
public final class ScorePolicy {
    private ScorePolicy() {
    }

    public static int flexPoints(double ratio) {
        if (ratio <= 40.0) {
            return 40;
        }
        if (ratio >= 100.0) {
            return 0;
        }
        return (int) Math.round(40 - (ratio - 40) * 40 / 60);
    }

    public static int budgetPoints(int kept, int total) {
        if (total <= 0) {
            return 12;
        }
        return (int) Math.round(25.0 * kept / total);
    }

    /** 전월 유동지출 대비 증감률(%). 전월이 없거나 전월 유동지출이 0이면 null. */
    public static Double changeRate(boolean hasPrevious, long previousVariable, long currentVariable) {
        if (!hasPrevious || previousVariable <= 0) {
            return null;
        }
        return WarningPolicy.round1((currentVariable - previousVariable) * 100.0 / previousVariable);
    }

    public static int changePoints(Double changeRate) {
        if (changeRate == null) {
            return 10;
        }
        if (changeRate <= -10) {
            return 20;
        }
        if (changeRate <= 0) {
            return 15;
        }
        if (changeRate <= 10) {
            return 10;
        }
        if (changeRate <= 30) {
            return 5;
        }
        return 0;
    }

    public static int peerPoints(double ratio, Double peerRatio) {
        if (peerRatio == null) {
            return 8;
        }
        if (ratio <= peerRatio) {
            return 15;
        }
        if (ratio <= peerRatio + 10) {
            return 10;
        }
        if (ratio <= peerRatio + 30) {
            return 5;
        }
        return 0;
    }

    public static String grade(int total) {
        if (total >= 85) {
            return "A";
        }
        if (total >= 70) {
            return "B";
        }
        if (total >= 55) {
            return "C";
        }
        return "D";
    }

    public static String message(double ratio, int flex, int budget, int budgetCount, int peer) {
        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.US, "유동지출 비율 %.1f%%", ratio));
        if (flex <= 10) {
            text.append("로 높습니다.");
        } else if (flex < 40) {
            text.append("입니다. 유동지출을 조금 더 줄여 보세요.");
        } else {
            text.append("로 안정적입니다.");
        }
        if (budgetCount == 0) {
            text.append(" 이번 달 예산이 없습니다.");
        } else if (budget < 25) {
            text.append(" 예산을 넘긴 카테고리가 있습니다.");
        } else {
            text.append(" 설정한 예산을 모두 지켰습니다.");
        }
        if (peer == 0) {
            text.append(" 또래 평균보다 유동비율이 높습니다.");
        } else if (peer >= 15) {
            text.append(" 또래 평균 이하입니다.");
        }
        return text.toString();
    }
}
