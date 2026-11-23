package com.chagok.server.dao;

import com.chagok.server.util.DBConnection;
import com.chagok.server.util.DbException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HabitScoreDAO {
    public Integer findScore(int userId, String ym) {
        String sql = "SELECT score FROM habit_score WHERE user_id=? AND score_month=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ym);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public void upsert(int userId, String ym, int score, String grade, double flexRatio, String message) {
        String sql = """
                INSERT INTO habit_score (user_id, score_month, score, grade, flex_ratio, message)
                VALUES (?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE score=VALUES(score), grade=VALUES(grade),
                    flex_ratio=VALUES(flex_ratio), message=VALUES(message)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ym);
            ps.setInt(3, score);
            ps.setString(4, grade);
            ps.setDouble(5, flexRatio);
            ps.setString(6, trim(message));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    private static String trim(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 200 ? message : message.substring(0, 200);
    }
}
