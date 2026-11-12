package com.chagok.server.dao;

import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.server.util.DBConnection;
import com.chagok.server.util.DbException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {
    private static final String SELECT = """
            SELECT t.transaction_id, t.category_id, c.name AS category_name, c.type AS category_type,
                   t.amount, t.transaction_date, t.fixed_type, t.repeat_cycle, t.memo,
                   t.parent_tx_id, t.is_auto
            FROM `transaction` t
            JOIN category c ON c.category_id = t.category_id
            """;

    public TransactionDTO findById(int transactionId) {
        String sql = SELECT + " WHERE t.transaction_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return TxMapper.map(rs);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public Integer ownerOf(int transactionId) {
        String sql = "SELECT user_id FROM `transaction` WHERE transaction_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
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

    public int insert(int userId, TransactionDTO tx) {
        String sql = """
                INSERT INTO `transaction`
                (user_id, category_id, amount, transaction_date, fixed_type, repeat_cycle, memo, parent_tx_id, is_auto)
                VALUES (?,?,?,?,?,?,?,NULL,0)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindWrite(ps, userId, tx);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public void update(int userId, TransactionDTO tx) {
        String sql = """
                UPDATE `transaction`
                SET category_id=?, amount=?, transaction_date=?, fixed_type=?, repeat_cycle=?, memo=?
                WHERE transaction_id=? AND user_id=?
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tx.getCategoryId());
            ps.setInt(2, tx.getAmount());
            ps.setDate(3, Date.valueOf(tx.getTransactionDate()));
            ps.setString(4, tx.getFixedType());
            ps.setString(5, tx.getRepeatCycle());
            ps.setString(6, tx.getMemo());
            ps.setInt(7, tx.getTransactionId());
            ps.setInt(8, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public int delete(int userId, int transactionId, boolean deleteAuto) {
        try (Connection connection = DBConnection.get()) {
            connection.setAutoCommit(false);
            int deleted = 0;
            try {
                if (deleteAuto) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM `transaction` WHERE user_id=? AND parent_tx_id=?")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, transactionId);
                        deleted += ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM `transaction` WHERE user_id=? AND transaction_id=?")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, transactionId);
                    deleted += ps.executeUpdate();
                }
                connection.commit();
                return deleted;
            } catch (SQLException ex) {
                connection.rollback();
                throw new DbException(ex);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<TransactionDTO> listBetween(int userId, LocalDate from, LocalDate to) {
        String sql = SELECT + """
                 WHERE t.user_id=? AND t.transaction_date BETWEEN ? AND ?
                 ORDER BY t.transaction_date, t.transaction_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionDTO> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(TxMapper.map(rs));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public MonthlyStatDTO monthlySummary(int userId, String ym) {
        String sql = """
                SELECT income, fixed_expense, variable_expense
                FROM v_monthly_summary WHERE user_id=? AND ym=?
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, ym);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new MonthlyStatDTO(ym, 0, 0, 0);
                }
                return new MonthlyStatDTO(ym, rs.getLong(1), rs.getLong(2), rs.getLong(3));
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public Map<Integer, Long> sumByCategory(int userId, String type, LocalDate from, LocalDate to) {
        String sql = """
                SELECT t.category_id, SUM(t.amount)
                FROM `transaction` t
                JOIN category c ON c.category_id = t.category_id
                WHERE t.user_id=? AND c.type=? AND t.transaction_date BETWEEN ? AND ?
                GROUP BY t.category_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            return readSums(ps);
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public Map<Integer, Long> peerCategorySum(int ageFrom, int ageTo, String gender, int excludeUserId,
                                              LocalDate from, LocalDate to) {
        String sql = """
                SELECT t.category_id, SUM(t.amount)
                FROM `transaction` t
                JOIN category c ON c.category_id = t.category_id
                JOIN `user` u ON u.user_id = t.user_id
                WHERE c.type='EXPENSE'
                  AND t.transaction_date BETWEEN ? AND ?
                  AND u.age BETWEEN ? AND ?
                  AND u.user_id <> ?
                  AND (? IS NULL OR u.gender = ?)
                GROUP BY t.category_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setInt(3, ageFrom);
            ps.setInt(4, ageTo);
            ps.setInt(5, excludeUserId);
            ps.setString(6, gender);
            ps.setString(7, gender);
            return readSums(ps);
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<long[]> peerMonthlyRows(String ym, int ageFrom, int ageTo, String gender, int excludeUserId) {
        String sql = """
                SELECT s.income, s.fixed_expense, s.variable_expense
                FROM v_monthly_summary s
                JOIN `user` u ON u.user_id = s.user_id
                WHERE s.ym=? AND u.age BETWEEN ? AND ? AND u.user_id <> ?
                  AND (? IS NULL OR u.gender = ?)
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ym);
            ps.setInt(2, ageFrom);
            ps.setInt(3, ageTo);
            ps.setInt(4, excludeUserId);
            ps.setString(5, gender);
            ps.setString(6, gender);
            try (ResultSet rs = ps.executeQuery()) {
                List<long[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)});
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    private static Map<Integer, Long> readSums(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            Map<Integer, Long> sums = new HashMap<>();
            while (rs.next()) {
                sums.put(rs.getInt(1), rs.getLong(2));
            }
            return sums;
        }
    }

    private static void bindWrite(PreparedStatement ps, int userId, TransactionDTO tx) throws SQLException {
        ps.setInt(1, userId);
        ps.setInt(2, tx.getCategoryId());
        ps.setInt(3, tx.getAmount());
        ps.setDate(4, Date.valueOf(tx.getTransactionDate()));
        ps.setString(5, tx.getFixedType());
        ps.setString(6, tx.getRepeatCycle());
        ps.setString(7, tx.getMemo());
    }
}
