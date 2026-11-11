package com.chagok.server.dao;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.server.util.DBConnection;
import com.chagok.server.util.DbException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    public CategoryDTO findById(int categoryId) {
        String sql = """
                SELECT category_id, type, name, default_fixed_type
                FROM category WHERE category_id=?
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return map(rs);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<CategoryDTO> findAll(String type) {
        String sql = """
                SELECT category_id, type, name, default_fixed_type
                FROM category
                WHERE (? IS NULL OR type = ?)
                ORDER BY sort_order, category_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                List<CategoryDTO> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(map(rs));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<CategoryDTO> findByType(String type) {
        return findAll(type);
    }

    private static CategoryDTO map(ResultSet rs) throws SQLException {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(rs.getInt("category_id"));
        dto.setType(rs.getString("type"));
        dto.setName(rs.getString("name"));
        dto.setDefaultFixedType(rs.getString("default_fixed_type"));
        return dto;
    }
}
