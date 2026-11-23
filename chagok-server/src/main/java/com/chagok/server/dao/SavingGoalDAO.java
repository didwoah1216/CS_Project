package com.chagok.server.dao;

import com.chagok.common.dto.GoalDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.GoalPolicy;
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
import java.util.List;

public class SavingGoalDAO {
    public int countActive(int userId) {
        String sql = "SELECT COUNT(*) FROM saving_goal WHERE user_id=? AND status='ACTIVE'";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public int insert(int userId, String name, int targetAmount, LocalDate start, LocalDate target) {
        String sql = """
                INSERT INTO saving_goal
                (user_id, goal_name, target_amount, saved_amount, start_date, target_date, status)
                VALUES (?,?,?,0,?,?,'ACTIVE')
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setInt(3, targetAmount);
            ps.setDate(4, Date.valueOf(start));
            ps.setDate(5, Date.valueOf(target));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public Integer ownerOf(int goalId) {
        String sql = "SELECT user_id FROM saving_goal WHERE goal_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, goalId);
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

    public GoalDTO find(int goalId) {
        String sql = """
                SELECT goal_id, goal_name, target_amount, saved_amount, start_date, target_date, status
                FROM saving_goal WHERE goal_id=?
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                GoalDTO goal = map(rs);
                goal.setDeposits(deposits(goalId));
                return goal;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<GoalDTO> list(int userId) {
        String sql = """
                SELECT goal_id, goal_name, target_amount, saved_amount, start_date, target_date, status
                FROM saving_goal WHERE user_id=?
                ORDER BY FIELD(status, 'ACTIVE', 'ACHIEVED', 'CANCELED'), target_date, goal_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<GoalDTO> items = new ArrayList<>();
                while (rs.next()) {
                    GoalDTO goal = map(rs);
                    goal.setDeposits(deposits(goal.getGoalId()));
                    items.add(goal);
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public List<GoalDTO.DepositDTO> deposits(int goalId) {
        String sql = """
                SELECT deposit_id, amount, deposit_date, memo
                FROM goal_deposit WHERE goal_id=?
                ORDER BY deposit_date, deposit_id
                """;
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                List<GoalDTO.DepositDTO> items = new ArrayList<>();
                while (rs.next()) {
                    GoalDTO.DepositDTO deposit = new GoalDTO.DepositDTO();
                    deposit.setDepositId(rs.getInt("deposit_id"));
                    deposit.setAmount(rs.getInt("amount"));
                    deposit.setDepositDate(rs.getDate("deposit_date").toLocalDate());
                    deposit.setMemo(rs.getString("memo"));
                    items.add(deposit);
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public int delete(int userId, int goalId) {
        String sql = "DELETE FROM saving_goal WHERE goal_id=? AND user_id=?";
        try (Connection connection = DBConnection.get();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    public void addDeposit(int goalId, int amount, LocalDate date, String memo) {
        String insert = """
                INSERT INTO goal_deposit (goal_id, amount, deposit_date, memo) VALUES (?,?,?,?)
                """;
        String sumSql = "SELECT COALESCE(SUM(amount),0) FROM goal_deposit WHERE goal_id=?";
        String update = "UPDATE saving_goal SET saved_amount=?, status=? WHERE goal_id=?";
        String statusSql = "SELECT target_amount, status FROM saving_goal WHERE goal_id=?";
        try (Connection connection = DBConnection.get()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(insert)) {
                    ps.setInt(1, goalId);
                    ps.setInt(2, amount);
                    ps.setDate(3, Date.valueOf(date));
                    ps.setString(4, memo);
                    ps.executeUpdate();
                }
                int saved;
                try (PreparedStatement ps = connection.prepareStatement(sumSql)) {
                    ps.setInt(1, goalId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        saved = rs.getInt(1);
                    }
                }
                if (saved < 0) {
                    connection.rollback();
                    throw new BizException(ErrorCode.E304, "저축액이 0원보다 작아질 수 없습니다");
                }
                int target;
                String status;
                try (PreparedStatement ps = connection.prepareStatement(statusSql)) {
                    ps.setInt(1, goalId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        target = rs.getInt(1);
                        status = rs.getString(2);
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(update)) {
                    ps.setInt(1, saved);
                    ps.setString(2, GoalPolicy.nextStatus(status, saved, target));
                    ps.setInt(3, goalId);
                    ps.executeUpdate();
                }
                connection.commit();
            } catch (BizException ex) {
                throw ex;
            } catch (SQLException ex) {
                connection.rollback();
                throw new DbException(ex);
            }
        } catch (SQLException ex) {
            throw new DbException(ex);
        }
    }

    private static GoalDTO map(ResultSet rs) throws SQLException {
        GoalDTO goal = new GoalDTO();
        goal.setGoalId(rs.getInt("goal_id"));
        goal.setGoalName(rs.getString("goal_name"));
        goal.setTargetAmount(rs.getInt("target_amount"));
        goal.setSavedAmount(rs.getInt("saved_amount"));
        goal.setStartDate(rs.getDate("start_date").toLocalDate());
        goal.setTargetDate(rs.getDate("target_date").toLocalDate());
        goal.setStatus(rs.getString("status"));
        return goal;
    }
}
