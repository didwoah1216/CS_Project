package com.chagok.server;

import com.chagok.client.AppContext;
import com.chagok.client.net.ApiException;
import com.chagok.client.net.ClientConfig;
import com.chagok.common.dto.BudgetDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.GoalDTO;
import com.chagok.common.dto.ScoreDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.dto.UserDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.dto.WeekdayStatDTO;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.protocol.JsonUtil;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserDAO;
import com.chagok.server.dao.UserSettingDAO;
import com.chagok.server.service.CompareService;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.GoalPolicy;
import com.chagok.server.util.DBConnection;
import com.chagok.server.util.ServerConfig;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1DatabaseTest {
    private static ChagokServer server;
    private static AppContext app;

    @BeforeAll
    static void start() throws Exception {
        String password = System.getenv("CHAGOK_DB_PASSWORD");
        Assumptions.assumeTrue(password != null && !password.isBlank(), "CHAGOK_DB_PASSWORD 없음");
        Properties file = new Properties();
        file.setProperty("server.port", "0");
        file.setProperty("db.host", env("CHAGOK_DB_HOST", "127.0.0.1"));
        file.setProperty("db.port", env("CHAGOK_DB_PORT", "3306"));
        file.setProperty("db.name", env("CHAGOK_DB_NAME", "chagok"));
        file.setProperty("db.user", env("CHAGOK_DB_USER", "chagok_app"));
        file.setProperty("db.password", password);
        file.setProperty("db.ssl", "false");
        file.setProperty("test.mode", "true");
        file.setProperty("test.login_id", "test01");
        ServerConfig config = ServerConfig.resolve(file, Map.of());
        ServerConfig.install(config);
        server = new ChagokServer(config);
        server.start();
        app = new AppContext(new ClientConfig("127.0.0.1", server.port(), 10000));
        app.connection.connect();
    }

    @AfterAll
    static void stop() {
        if (app != null) {
            app.connection.close();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void viewAndWarningMatchTheReport() throws Exception {
        try (Connection connection = DBConnection.get();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT income, fixed_expense, variable_expense
                     FROM v_monthly_summary WHERE user_id=1 AND ym='2026-10'
                     """)) {
            assertTrue(rs.next());
            assertEquals(1_282_300, rs.getLong("income"));
            assertEquals(429_400, rs.getLong("fixed_expense"));
            assertEquals(776_300, rs.getLong("variable_expense"));
        }
        UserDTO user = app.api.testStart();
        assertEquals("test01", user.getLoginId());
        assertEquals(23, user.getAge());
        WarningDTO first = app.api.warning("2026-10");
        assertEquals("WARNING", first.getLevel());
        assertEquals(91.0, first.getRatio());
        assertEquals(55.6, first.getPeerAvgRatio());
        assertEquals(852_900, first.getAvailable());
        assertEquals(776_300, first.getVariable());
        assertEquals("이번 달 유동지출 비율 91.0%, 기준(70%) 초과. 쇼핑·술/유흥 지출 조절 필요", first.getMessage());
        assertTrue(first.isShowPopup());
        assertEquals("쇼핑", first.getTopCategories().get(0).getName());
        assertEquals("술/유흥", first.getTopCategories().get(1).getName());
        assertFalse(app.api.warning("2026-10").isShowPopup());
        assertEquals(74.9, app.api.warning("2026-09").getRatio());
    }

    @Test
    void statisticsAndPeerComparisonMatchTheReport() {
        app.api.testStart();
        JsonObject category = app.api.categoryStat("2026-10");
        assertEquals(1_205_700, category.get("total").getAsLong());
        CategoryStatDTO housing = app.api.statItems(category.get("items")).get(0);
        assertEquals("주거/관리비", housing.getName());
        assertEquals(29.0, housing.getRatio());
        JsonObject monthly = app.api.monthly("2026-10");
        assertEquals(25_300, monthly.get("diffAmount").getAsLong());
        assertEquals(2.1, monthly.get("diffRate").getAsDouble());
        CompareDTO compare = app.api.compare("2026-10", false);
        assertEquals(20, compare.getAgeGroup());
        assertEquals(10, compare.getSampleSize());
        assertEquals(91.0, compare.getMyRatio());
        assertEquals(55.6, compare.getPeerRatio());
        CategoryStatDTO food = compare.getItems().stream()
                .filter(item -> "식비".equals(item.getName())).findFirst().orElseThrow();
        assertEquals(304_500, food.getAmount());
        assertEquals(364_150, food.getPeerAvg());
        assertEquals(-59_650, food.getDiff());
        assertFalse(JsonUtil.toJson(compare).contains("김민준"));
        CompareDTO men = app.api.compare("2026-10", true);
        assertEquals(5, men.getSampleSize());
    }

    @Test
    void sixMonthTrendEndsAtTheSelectedMonth() {
        app.api.testStart();
        List<com.chagok.common.dto.MonthlyStatDTO> months = app.api.months(app.api.trend("2026-10").get("items"));
        assertEquals(6, months.size());
        assertEquals("2026-05", months.get(0).getYm());
        assertEquals(0, months.get(0).getIncome());
        assertEquals("2026-09", months.get(4).getYm());
        assertEquals(751_000, months.get(4).getVariable());
        assertEquals(1_432_700, months.get(4).getIncome());
        assertEquals("2026-10", months.get(5).getYm());
        assertEquals(776_300, months.get(5).getVariable());
        assertEquals(1_282_300, months.get(5).getIncome());
        assertEquals(429_400, months.get(5).getFixed());
    }

    @Test
    void habitScoreMatchesOctoberAndSeptember() throws Exception {
        app.api.testStart();
        try {
            ScoreDTO october = app.api.score("2026-10", false);
            assertEquals(33, october.getScore());
            assertEquals("D", october.getGrade());
            assertEquals(91.0, october.getFlexRatio());
            assertEquals(44, october.getPrevScore());
            assertEquals(6, october.getItems().get(0).getPoint());
            assertEquals(17, october.getItems().get(1).getPoint());
            assertEquals(10, october.getItems().get(2).getPoint());
            assertEquals(0, october.getItems().get(3).getPoint());
            ScoreDTO september = app.api.score("2026-09", false);
            assertEquals(44, september.getScore());
            assertEquals("D", september.getGrade());
            assertEquals(74.9, september.getFlexRatio());
            assertNull(september.getPrevScore());
            assertEquals(17, september.getItems().get(0).getPoint());
            assertEquals(12, september.getItems().get(1).getPoint());
            assertEquals(5, september.getItems().get(3).getPoint());
            app.api.score("2026-10", true);
            try (Connection connection = DBConnection.get();
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("""
                         SELECT score, grade, flex_ratio FROM habit_score
                         WHERE user_id=1 AND score_month='2026-10'
                         """)) {
                assertTrue(rs.next());
                assertEquals(33, rs.getInt("score"));
                assertEquals("D", rs.getString("grade"));
                assertEquals(91.0, rs.getDouble("flex_ratio"));
            }
        } finally {
            try (Connection connection = DBConnection.get();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM habit_score WHERE user_id=1 AND score_month='2026-10'");
            }
        }
    }

    @Test
    void savingGoalsMatchTheSampleAndAcceptADeposit() {
        app.api.testStart();
        List<GoalDTO> items = app.api.goals(app.api.goalList().get("items"));
        assertEquals(2, items.size());
        GoalDTO trip = items.stream().filter(item -> "겨울방학 제주도 여행".equals(item.getGoalName())).findFirst().orElseThrow();
        assertEquals(500_000, trip.getTargetAmount());
        assertEquals(120_000, trip.getSavedAmount());
        assertEquals(24.0, trip.getProgress());
        assertEquals("ACTIVE", trip.getStatus());
        assertEquals(2, trip.getDeposits().size());
        assertEquals(GoalPolicy.monthlyNeed(120_000, 500_000, DateUtil.today(), trip.getTargetDate()), trip.getMonthlyNeed());
        assertEquals(GoalPolicy.dDay(DateUtil.today(), trip.getTargetDate()), trip.getDDay());
        GoalDTO created = null;
        try {
            ApiException past = assertThrows(ApiException.class, () -> app.api.goalAdd(
                    "잘못된 일정", 10_000, "2026-12-01", "2026-11-01"));
            assertEquals(ErrorCode.E304, past.getCode());
            created = app.api.goalAdd("시험용 비상금", 100_000, "2026-11-01", "2027-05-01");
            assertEquals(0, created.getSavedAmount());
            GoalDTO after = app.api.goalDeposit(created.getGoalId(), 40_000, "2026-11-23", "첫 입금");
            assertEquals(40_000, after.getSavedAmount());
            assertEquals(40.0, after.getProgress());
            assertEquals("ACTIVE", after.getStatus());
            GoalDTO done = app.api.goalDeposit(created.getGoalId(), 60_000, "2026-11-23", null);
            assertEquals(100_000, done.getSavedAmount());
            assertEquals("ACHIEVED", done.getStatus());
            ApiException overdraw = assertThrows(ApiException.class,
                    () -> app.api.goalDeposit(done.getGoalId(), -200_000, "2026-11-23", "과한 인출"));
            assertEquals(ErrorCode.E304, overdraw.getCode());
        } finally {
            if (created != null && created.getGoalId() != null) {
                app.api.goalDelete(created.getGoalId());
            }
        }
        assertEquals(2, app.api.goals(app.api.goalList().get("items")).size());
    }

    @Test
    void weekdayPatternMatchesTheReportRange() {
        app.api.testStart();
        JsonObject body = app.api.weekday("2026-09-01", "2026-10-29");
        assertEquals(1_527_300, body.get("total").getAsLong());
        assertEquals(762_600, body.get("weekendAmount").getAsLong());
        assertEquals(49.9, body.get("weekendRatio").getAsDouble());
        assertFalse(body.get("concentrated").getAsBoolean());
        WeekdayStatDTO saturday = app.api.weekdays(body.get("items")).stream()
                .filter(item -> "토".equals(item.getName())).findFirst().orElseThrow();
        assertEquals(328_300, saturday.getAmount());
        assertEquals(21.5, saturday.getRatio());
        assertTrue(body.get("message").getAsString().contains("기준(50%) 미만"));
    }

    @Test
    void peerSampleUnderThreeIsE401() {
        CompareService compare = new CompareService(new UserDAO(), new CategoryDAO(),
                new TransactionDAO(), new UserSettingDAO());
        com.chagok.common.protocol.BizException ex = assertThrows(
                com.chagok.common.protocol.BizException.class,
                () -> compare.compare(12, "2026-10", false));
        assertEquals(ErrorCode.E401, ex.getCode());
    }

    @Test
    void expenseWithoutFixedTypeIsRejectedAndResetRestoresDummy() {
        app.api.testStart();
        Map<String, Object> missing = new LinkedHashMap<>();
        missing.put("categoryId", 5);
        missing.put("amount", 8500);
        missing.put("transactionDate", "2026-11-12");
        ApiException rejected = assertThrows(ApiException.class, () -> app.api.addTransaction(missing));
        assertEquals(ErrorCode.E102, rejected.getCode());
        assertTrue(rejected.getMessage().contains("고정/유동"));

        Map<String, Object> added = new LinkedHashMap<>();
        added.put("categoryId", 5);
        added.put("amount", 8500);
        added.put("transactionDate", "2026-11-12");
        added.put("fixedType", "VARIABLE");
        added.put("memo", "학식");
        TransactionDTO saved = null;
        try {
            saved = app.api.addTransaction(added);
            assertTrue(saved.getTransactionId() > 0);
            assertEquals("식비", saved.getCategoryName());
            assertEquals("VARIABLE", saved.getFixedType());
        } finally {
            if (saved != null && saved.getTransactionId() != null) {
                try {
                    app.api.deleteTransaction(saved.getTransactionId(), false);
                } catch (RuntimeException ignored) {
                    // 아래 초기화가 더미 건수를 되돌린다.
                }
            }
            JsonObject reset = app.api.resetDummy();
            assertEquals(13, reset.get("resetUsers").getAsInt());
            assertEquals(2284, reset.get("resetTx").getAsInt());
        }
    }

    @Test
    void octoberBudgetsMatchTheReport() {
        app.api.testStart();
        JsonObject body = app.api.budgetList("2026-10");
        assertEquals(80, body.get("alertRatio").getAsInt());
        List<BudgetDTO> items = app.api.budgets(body.get("items"));
        assertEquals(6, items.size());
        assertBudget(items, "식비", 350_000, 304_500, 87.0, "ALERT");
        assertBudget(items, "카페/간식", 60_000, 58_600, 97.7, "ALERT");
        assertBudget(items, "교통비", 60_000, 49_700, 82.8, "ALERT");
        assertBudget(items, "쇼핑", 100_000, 145_500, 145.5, "OVER");
        assertBudget(items, "문화/여가", 60_000, 22_400, 37.3, "NORMAL");
        assertBudget(items, "술/유흥", 80_000, 149_500, 186.9, "OVER");
    }

    @Test
    void budgetUpsertCopyAndIncomeRejected() {
        app.api.testStart();
        BudgetDTO created = null;
        try {
            ApiException income = assertThrows(ApiException.class, () -> app.api.budgetSet(1, "2026-12", 10_000));
            assertEquals(ErrorCode.E304, income.getCode());
            ApiException empty = assertThrows(ApiException.class, () -> app.api.budgetCopy("2026-09"));
            assertEquals(ErrorCode.E301, empty.getCode());
            created = app.api.budgetSet(13, "2026-12", 40_000);
            assertEquals(0.0, created.getUsageRate());
            assertEquals("NORMAL", created.getStatus());
            BudgetDTO updated = app.api.budgetSet(13, "2026-12", 50_000);
            assertEquals(created.getBudgetId(), updated.getBudgetId());
            assertEquals(50_000, updated.getAmount());
            JsonObject copied = app.api.budgetCopy("2026-12");
            assertEquals(4, copied.get("copied").getAsInt());
            List<BudgetDTO> december = app.api.budgets(copied.get("items"));
            assertBudget(december, "식비", 330_000, 0, 0.0, "NORMAL");
            assertBudget(december, "교육/자기계발", 50_000, 0, 0.0, "NORMAL");
            assertEquals(350_000, app.api.budgets(app.api.budgetList("2026-10").get("items")).stream()
                    .filter(item -> "식비".equals(item.getCategoryName())).findFirst().orElseThrow().getAmount());
        } finally {
            if (created != null) {
                for (BudgetDTO item : app.api.budgets(app.api.budgetList("2026-12").get("items"))) {
                    app.api.budgetDelete(item.getBudgetId());
                }
            }
        }
    }

    @Test
    void recurringRunFillsNovemberAndSkipsExistingOctober() {
        app.api.testStart();
        try {
            JsonObject october = app.api.recurringRun("2026-10");
            assertEquals(0, october.get("created").getAsInt());
            assertEquals(6, october.get("skipped").getAsInt());
            JsonObject november = app.api.recurringRun("2026-11");
            assertEquals(6, november.get("created").getAsInt());
            JsonObject again = app.api.recurringRun("2026-11");
            assertEquals(0, again.get("created").getAsInt());
            assertEquals(6, again.get("skipped").getAsInt());
            TransactionDTO rent = app.api.transactions(november.get("items")).stream()
                    .filter(item -> "월세".equals(item.getMemo())).findFirst().orElseThrow();
            assertEquals(350_000, rent.getAmount());
            assertEquals(LocalDate.of(2026, 11, 5), rent.getTransactionDate());
            assertEquals(5, rent.getParentTxId());
            assertTrue(rent.isAuto());
            assertEquals("NONE", rent.getRepeatCycle());
            assertEquals("FIXED", rent.getFixedType());
        } finally {
            JsonObject reset = app.api.resetDummy();
            assertEquals(2284, reset.get("resetTx").getAsInt());
        }
    }

    @Test
    void searchFiltersPeriodCategoryAmountAndKeyword() {
        app.api.testStart();
        Map<String, Object> food = new LinkedHashMap<>();
        food.put("from", "2026-10-01");
        food.put("to", "2026-10-31");
        food.put("keyword", "학식");
        food.put("categoryId", 5);
        JsonObject meals = app.api.search(food);
        assertEquals(4, meals.get("count").getAsInt());
        for (TransactionDTO item : app.api.transactions(meals.get("items"))) {
            assertEquals("식비", item.getCategoryName());
            assertTrue(item.getMemo().contains("학식"));
        }
        Map<String, Object> band = new LinkedHashMap<>();
        band.put("from", "2026-10-01");
        band.put("to", "2026-10-31");
        band.put("minAmount", 300_000);
        band.put("maxAmount", 350_000);
        assertEquals(2, app.api.search(band).get("count").getAsInt());
        Map<String, Object> none = new LinkedHashMap<>();
        none.put("keyword", "없는메모xyz");
        assertEquals(0, app.api.search(none).get("count").getAsInt());
        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("from", "2026-10-31");
        reversed.put("to", "2026-10-01");
        ApiException bad = assertThrows(ApiException.class, () -> app.api.search(reversed));
        assertEquals(ErrorCode.E103, bad.getCode());
    }

    private static void assertBudget(List<BudgetDTO> items, String name, int amount, long used,
                                     double rate, String status) {
        BudgetDTO item = items.stream().filter(row -> name.equals(row.getCategoryName())).findFirst().orElseThrow();
        assertEquals(amount, item.getAmount());
        assertEquals(used, item.getUsed());
        assertEquals(rate, item.getUsageRate());
        assertEquals(status, item.getStatus());
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
