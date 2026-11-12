package com.chagok.common.util;

import java.util.List;
import java.util.Locale;

/**
 * FR-7 과소비 경고 판정.
 * SAFE &lt; 50, CAUTION 50~기준 미만, WARNING 기준 이상 또는 또래+허용폭 초과, DANGER 100 이상 또는 가용소득 없음.
 */
public final class WarningPolicy {
    public enum Level {
        SAFE, CAUTION, WARNING, DANGER;

        public boolean atLeast(Level other) {
            return ordinal() >= other.ordinal();
        }
    }

    public static final class Decision {
        public final Level level;
        public final Double ratio;
        public final long available;
        public final String message;

        public Decision(Level level, Double ratio, long available, String message) {
            this.level = level;
            this.ratio = ratio;
            this.available = available;
            this.message = message;
        }
    }

    private WarningPolicy() {
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static Level levelOf(Double ratio, int warnRatio, Double peerAvg, int peerGap,
                                boolean noIncome, boolean noAvailable) {
        if (noIncome || noAvailable || ratio == null) {
            return Level.DANGER;
        }
        if (ratio >= 100.0) {
            return Level.DANGER;
        }
        if (ratio >= warnRatio || (peerAvg != null && ratio > peerAvg + peerGap)) {
            return Level.WARNING;
        }
        if (ratio >= 50.0) {
            return Level.CAUTION;
        }
        return Level.SAFE;
    }

    public static Decision decide(long income, long fixed, long variable,
                                  int warnRatio, int peerGap, Double peerAvg,
                                  List<String> topNames) {
        long available = income - fixed;
        boolean noIncome = income == 0;
        boolean noAvailable = !noIncome && available <= 0;
        Double ratio = null;
        if (!noIncome && !noAvailable) {
            ratio = round1(variable * 100.0 / available);
        }
        Level level = levelOf(ratio, warnRatio, peerAvg, peerGap, noIncome, noAvailable);
        String message = message(level, ratio, warnRatio, topNames, noIncome, noAvailable);
        return new Decision(level, ratio, available, message);
    }

    public static String message(Level level, Double ratio, int warnRatio, List<String> topNames,
                                 boolean noIncome, boolean noAvailable) {
        if (noIncome) {
            return "이번 달 수입 내역이 없어 비율 계산 불가";
        }
        if (noAvailable) {
            return "가용소득 없음(수입 ≤ 고정지출)";
        }
        String ratioText = String.format(Locale.US, "%.1f", ratio);
        String names = topNames == null || topNames.isEmpty() ? "" : String.join("·", topNames);
        String tail = names.isEmpty() ? "" : " " + names + " 지출 조절 필요";
        return switch (level) {
            case DANGER -> "이번 달 유동지출 비율 " + ratioText + "%, 가용소득 이상을 썼습니다." + tail;
            case WARNING -> {
                String head = ratio != null && ratio < warnRatio
                        ? "이번 달 유동지출 비율 " + ratioText + "%, 또래 평균보다 높습니다."
                        : "이번 달 유동지출 비율 " + ratioText + "%, 기준(" + warnRatio + "%) 초과.";
                yield head + tail;
            }
            case CAUTION -> "이번 달 유동지출 비율 " + ratioText + "%입니다. 기준(" + warnRatio + "%)에 가까워지고 있습니다.";
            case SAFE -> "이번 달 유동지출 비율 " + ratioText + "%로 안정적인 소비입니다.";
        };
    }
}
