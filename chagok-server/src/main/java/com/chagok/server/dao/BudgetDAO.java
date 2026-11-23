package com.chagok.server.dao;

import com.chagok.common.dto.BudgetDTO;
import com.chagok.server.util.DBConnection;
import com.chagok.server.util.DbException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {
    private static final String SELECT = """
            SELECT b.budget_id, b.budget_month, b.category_id, c.name AS category_name,
                   b.amount, COALESCE(SUM(t.amount), 0) AS used
            FROM budget b
            JOIN category c ON c.category_id = b.category_id
            LEFT JOIN `transaction` t
              ON t.user_id = b.user_id
             AND t.category_id = b.category_id
             AND DATE_FORMAT(t.transaction_date, '%Y-%m') = b.budget_month
            """;

    public List<BudgetDTO> findByMonth(int userId, String ym) {
        String sql = SELECT + """
                 WHERE b.user_id=? AND b.budget_month=?
                 GROUP BY b.budget_id, b.budget_month, b.category_id, c.name, c.sort_order, b.amount
                 ORDER BY c.sort_order, b.budget_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ym);
            try (ResultSet rs = ps.executeQuery()) {
                List<BudgetDTO> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(map(rs));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public BudgetDTO findByCategoryMonth(int userId, int categoryId, String ym) {
        String sql = SELECT + """
                 WHERE b.user_id=? AND b.category_id=? AND b.budget_month=?
                 GROUP BY b.budget_id, b.budget_month, b.category_id, c.name, c.sort_order, b.amount
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setString(3, ym);
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

    public Integer ownerOf(int budgetId) {
        String sql = "SELECT user_id FROM budget WHERE budget_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, budgetId);
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

    public void upsert(int userId, int categoryId, String ym, int amount) {
        String sql = """
                INSERT INTO budget (user_id, category_id, budget_month, amount)
                VALUES (?,?,?,?)
                ON DUPLICATE KEY UPDATE amount=VALUES(amount)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setString(3, ym);
            ps.setInt(4, amount);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public int delete(int userId, int budgetId) {
        String sql = "DELETE FROM budget WHERE budget_id=? AND user_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public int countMonth(int userId, String ym) {
        String sql = "SELECT COUNT(*) FROM budget WHERE user_id=? AND budget_month=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ym);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    /**
     * 전월 예산을 대상 월로 넣는다. 같은 카테고리가 있으면 금액을 덮어쓰고, 대상 월에만 있는 예산은 둔다.
     */
    public void copyMonth(int userId, String fromYm, String toYm) {
        String sql = """
                INSERT INTO budget (user_id, category_id, budget_month, amount)
                SELECT user_id, category_id, ?, amount
                FROM budget
                WHERE user_id=? AND budget_month=?
                ON DUPLICATE KEY UPDATE amount=VALUES(amount)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, toYm);
            ps.setInt(2, userId);
            ps.setString(3, fromYm);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    private static BudgetDTO map(ResultSet rs) throws SQLException {
        BudgetDTO dto = new BudgetDTO();
        dto.setBudgetId(rs.getInt("budget_id"));
        dto.setBudgetMonth(rs.getString("budget_month"));
        dto.setCategoryId(rs.getInt("category_id"));
        dto.setCategoryName(rs.getString("category_name"));
        dto.setAmount(rs.getInt("amount"));
        dto.setUsed(rs.getLong("used"));
        return dto;
    }
}
