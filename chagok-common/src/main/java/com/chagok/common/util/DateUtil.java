package com.chagok.common.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class DateUtil {
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final Pattern YM = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() {
    }

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }

    public static String formatNow() {
        return now().format(DATE_TIME);
    }

    public static YearMonth parseYm(String ym) {
        if (ym == null || !YM.matcher(ym).matches()) {
            throw new BizException(ErrorCode.E103, "월 형식은 yyyy-MM 이어야 합니다");
        }
        return YearMonth.parse(ym);
    }

    public static LocalDate[] monthRange(String ym) {
        YearMonth month = parseYm(ym);
        return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
    }

    public static String previousYm(String ym) {
        return parseYm(ym).minusMonths(1).toString();
    }

    public static LocalDate parseDate(String text, boolean present) {
        if (!present || text == null || text.isBlank()) {
            throw new BizException(ErrorCode.E102, "날짜를 입력해야 합니다");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.E103, "존재하지 않는 날짜입니다");
        }
        LocalDate max = today().plusYears(1);
        if (date.isBefore(MIN_DATE) || date.isAfter(max)) {
            throw new BizException(ErrorCode.E103, "날짜는 2000-01-01부터 오늘+1년 사이여야 합니다");
        }
        return date;
    }
}
