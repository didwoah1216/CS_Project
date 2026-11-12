package com.chagok.server.dao;

import com.chagok.common.dto.SettingDTO;
import com.chagok.server.util.DBConnection;
import com.chagok.server.util.DbException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserSettingDAO {
    public SettingDTO findByUser(int userId) {
        String sql = """
                SELECT warn_enabled, warn_ratio, peer_gap_ratio, budget_alert_ratio, compare_same_gender
                FROM user_setting WHERE user_id=?
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                SettingDTO dto = new SettingDTO();
                dto.setWarnEnabled(rs.getInt("warn_enabled") == 1);
                dto.setWarnRatio(rs.getInt("warn_ratio"));
                dto.setPeerGapRatio(rs.getInt("peer_gap_ratio"));
                dto.setBudgetAlertRatio(rs.getInt("budget_alert_ratio"));
                dto.setCompareSameGender(rs.getInt("compare_same_gender") == 1);
                return dto;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public void update(int userId, SettingDTO setting) {
        String update = """
                UPDATE user_setting
                SET warn_enabled=?, warn_ratio=?, peer_gap_ratio=?, budget_alert_ratio=?, compare_same_gender=?
                WHERE user_id=?
                """;
        String insert = """
                INSERT INTO user_setting
                (user_id, warn_enabled, warn_ratio, peer_gap_ratio, budget_alert_ratio, compare_same_gender)
                VALUES (?,?,?,?,?,?)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setInt(1, setting.isWarnEnabled() ? 1 : 0);
            ps.setInt(2, setting.getWarnRatio());
            ps.setInt(3, setting.getPeerGapRatio());
            ps.setInt(4, setting.getBudgetAlertRatio());
            ps.setInt(5, setting.isCompareSameGender() ? 1 : 0);
            ps.setInt(6, userId);
            if (ps.executeUpdate() > 0) {
                return;
            }
            try (PreparedStatement ins = connection.prepareStatement(insert)) {
                ins.setInt(1, userId);
                ins.setInt(2, setting.isWarnEnabled() ? 1 : 0);
                ins.setInt(3, setting.getWarnRatio());
                ins.setInt(4, setting.getPeerGapRatio());
                ins.setInt(5, setting.getBudgetAlertRatio());
                ins.setInt(6, setting.isCompareSameGender() ? 1 : 0);
                ins.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }
}
