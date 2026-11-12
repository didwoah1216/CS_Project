package com.chagok.server;

import com.chagok.client.AppContext;
import com.chagok.client.net.ApiException;
import com.chagok.client.net.ClientConfig;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.dto.UserDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.protocol.JsonUtil;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserDAO;
import com.chagok.server.dao.UserSettingDAO;
import com.chagok.server.service.CompareService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
