package com.chagok.client.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);

    private Money() {
    }

    public static String won(long amount) {
        return FORMAT.format(amount) + "원";
    }

    public static String comma(long amount) {
        return FORMAT.format(amount);
    }
}
