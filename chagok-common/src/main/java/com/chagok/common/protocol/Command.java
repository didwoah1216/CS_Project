package com.chagok.common.protocol;

/**
 * 중간보고서 기준 통신 명령 29개.
 * 프로토콜 명세서 v2의 AUTH_TEST_LOGIN 은 TEST_START 와 같은 명령으로 받는다.
 */
public enum Command {
    PING(false),
    TEST_START(false),
    CATEGORY_LIST(true),
    TX_ADD(true),
    TX_UPDATE(true),
    TX_DELETE(true),
    TX_LIST_MONTH(true),
    TX_LIST_DAY(true),
    TX_SEARCH(true),
    RECURRING_RUN(true),
    EXPORT_CSV(true),
    STAT_CATEGORY(true),
    STAT_MONTHLY(true),
    STAT_TREND(true),
    STAT_WEEKDAY(true),
    FEEDBACK_WARNING(true),
    FEEDBACK_SCORE(true),
    COMPARE_AGE_GROUP(true),
    BUDGET_SET(true),
    BUDGET_LIST(true),
    BUDGET_DELETE(true),
    BUDGET_COPY(true),
    GOAL_ADD(true),
    GOAL_LIST(true),
    GOAL_DEPOSIT(true),
    GOAL_DELETE(true),
    SETTING_GET(true),
    SETTING_UPDATE(true),
    DUMMY_RESET(true);

    private final boolean auth;

    Command(boolean auth) {
        this.auth = auth;
    }

    public boolean requiresAuth() {
        return auth;
    }

    public static Command parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        if ("AUTH_TEST_LOGIN".equals(name)) {
            return TEST_START;
        }
        try {
            return Command.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
