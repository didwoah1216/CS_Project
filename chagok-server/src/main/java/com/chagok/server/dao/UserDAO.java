package com.chagok.server.dao;

import com.chagok.common.dto.UserDTO;
import com.chagok.server.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public UserDTO findById(int userId) {
        return find("SELECT user_id, login_id, user_name, age, gender, is_dummy FROM `user` WHERE user_id=?", userId);
    }

    public UserDTO findByLoginId(String loginId) {
        return find("SELECT user_id, login_id, user_name, age, gender, is_dummy FROM `user` WHERE login_id=?", loginId);
    }

    public int countPeers(int ageFrom, int ageTo, String gender, int excludeUserId) {
        String sql = """
                SELECT COUNT(*) FROM `user`
                WHERE age BETWEEN ? AND ? AND user_id <> ? AND (? IS NULL OR gender = ?)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ageFrom);
            ps.setInt(2, ageTo);
            ps.setInt(3, excludeUserId);
            ps.setString(4, gender);
            ps.setString(5, gender);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new com.chagok.server.util.DbException(ex);
        }
    }

    private UserDTO find(String sql, Object key) {
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UserDTO dto = new UserDTO();
                dto.setUserId(rs.getInt("user_id"));
                dto.setLoginId(rs.getString("login_id"));
                dto.setUserName(rs.getString("user_name"));
                dto.setAge(rs.getInt("age"));
                dto.setGender(rs.getString("gender"));
                dto.setDummy(rs.getInt("is_dummy") == 1);
                return dto;
            }
        } catch (SQLException ex) {
            throw new com.chagok.server.util.DbException(ex);
        }
    }
}
