package com.chagok.server.util;

import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DummyDataLoader {
    private static final String[] TABLES = {
            "habit_score", "goal_deposit", "saving_goal", "budget", "user_setting",
            "`transaction`", "category", "`user`"
    };

    public int[] reset() {
        String script = readScript();
        try (Connection connection = DBConnection.get()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String table : TABLES) {
                    statement.execute("DELETE FROM " + table);
                }
                for (String sql : statements(script)) {
                    statement.execute(sql);
                }
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
                connection.commit();
                int users = count(statement, "SELECT COUNT(*) FROM `user`");
                int tx = count(statement, "SELECT COUNT(*) FROM `transaction`");
                int nextTx = count(statement, "SELECT COALESCE(MAX(transaction_id),0)+1 FROM `transaction`");
                statement.execute("ALTER TABLE `transaction` AUTO_INCREMENT = " + nextTx);
                int nextUser = count(statement, "SELECT COALESCE(MAX(user_id),0)+1 FROM `user`");
                statement.execute("ALTER TABLE `user` AUTO_INCREMENT = " + nextUser);
                return new int[]{users, tx};
            } catch (SQLException ex) {
                connection.rollback();
                throw new DbException(ex);
            }
        } catch (SQLException ex) {
            throw new BizException(ErrorCode.E501, ErrorCode.E501.getDefaultMessage());
        }
    }

    private static int count(Statement statement, String sql) throws SQLException {
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String readScript() {
        try (InputStream in = DummyDataLoader.class.getClassLoader().getResourceAsStream("sql/dummy_data.sql")) {
            if (in == null) {
                throw new BizException(ErrorCode.E501, "dummy_data.sql 을 찾을 수 없습니다");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.E501, "dummy_data.sql 을 읽지 못했습니다");
        }
    }

    static List<String> statements(String script) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        List<String> result = new ArrayList<>();
        for (String part : cleaned.toString().split(";")) {
            String sql = part.trim();
            if (sql.isEmpty()) {
                continue;
            }
            String upper = sql.toUpperCase();
            if (upper.startsWith("USE ") || upper.startsWith("START TRANSACTION") || upper.equals("COMMIT")) {
                continue;
            }
            result.add(sql);
        }
        return result;
    }
}
