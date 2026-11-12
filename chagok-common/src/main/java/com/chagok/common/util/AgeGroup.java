package com.chagok.common.util;

public final class AgeGroup {
    private final int from;
    private final int to;

    private AgeGroup(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public static AgeGroup of(int age) {
        int from = (age / 10) * 10;
        return new AgeGroup(from, from + 9);
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }
}
