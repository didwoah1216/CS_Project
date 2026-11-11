package com.chagok.server.dao;

import com.chagok.common.dto.TransactionDTO;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

final class TxMapper {
    private TxMapper() {
    }

    static TransactionDTO map(ResultSet rs) throws SQLException {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(rs.getInt("transaction_id"));
        dto.setCategoryId(rs.getInt("category_id"));
        dto.setCategoryName(rs.getString("category_name"));
        dto.setType(rs.getString("category_type"));
        dto.setAmount(rs.getInt("amount"));
        Date date = rs.getDate("transaction_date");
        dto.setTransactionDate(date == null ? null : date.toLocalDate());
        dto.setFixedType(rs.getString("fixed_type"));
        dto.setRepeatCycle(rs.getString("repeat_cycle"));
        dto.setMemo(rs.getString("memo"));
        int parent = rs.getInt("parent_tx_id");
        dto.setParentTxId(rs.wasNull() ? null : parent);
        dto.setAuto(rs.getInt("is_auto") == 1);
        return dto;
    }
}
