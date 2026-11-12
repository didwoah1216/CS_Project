package com.chagok.server.service;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.DailySumDTO;
import com.chagok.common.dto.TransactionDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.InputValidator;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.util.ServerLogger;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionService {
    private final TransactionDAO transactionDAO;
    private final CategoryDAO categoryDAO;

    public TransactionService(TransactionDAO transactionDAO, CategoryDAO categoryDAO) {
        this.transactionDAO = transactionDAO;
        this.categoryDAO = categoryDAO;
    }

    public Map<String, Object> add(int userId, JsonObject data) {
        TransactionDTO saved = transactionDAO.findById(transactionDAO.insert(userId, read(data, null)));
        return Map.of("transaction", saved);
    }

    public Map<String, Object> update(int userId, JsonObject data) {
        if (!JsonFields.present(data, "transactionId")) {
            throw new BizException(ErrorCode.E102, "내역 번호가 필요합니다");
        }
        int transactionId = JsonFields.integer(data, "transactionId");
        requireOwner(userId, transactionId);
        TransactionDTO draft = read(data, transactionId);
        transactionDAO.update(userId, draft);
        return Map.of("transaction", transactionDAO.findById(transactionId));
    }

    public Map<String, Object> delete(int userId, JsonObject data) {
        if (!JsonFields.present(data, "transactionId")) {
            throw new BizException(ErrorCode.E102, "내역 번호가 필요합니다");
        }
        int transactionId = JsonFields.integer(data, "transactionId");
        requireOwner(userId, transactionId);
        boolean deleteAuto = JsonFields.present(data, "deleteAuto") && JsonFields.bool(data, "deleteAuto", false);
        int deleted = transactionDAO.delete(userId, transactionId, deleteAuto);
        return Map.of("deletedCount", deleted);
    }

    public Map<String, Object> listMonth(int userId, String ym) {
        LocalDate[] range = DateUtil.monthRange(ym);
        List<TransactionDTO> items = transactionDAO.listBetween(userId, range[0], range[1]);
        Map<String, DailySumDTO> daily = new LinkedHashMap<>();
        long income = 0;
        long expense = 0;
        for (TransactionDTO item : items) {
            String date = item.getTransactionDate().toString();
            DailySumDTO sum = daily.computeIfAbsent(date, key -> new DailySumDTO(key, 0, 0));
            if ("INCOME".equals(item.getType())) {
                sum.setIncome(sum.getIncome() + item.getAmount());
                income += item.getAmount();
            } else {
                sum.setExpense(sum.getExpense() + item.getAmount());
                expense += item.getAmount();
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("daily", new ArrayList<>(daily.values()));
        body.put("monthIncome", income);
        body.put("monthExpense", expense);
        return body;
    }

    public Map<String, Object> listDay(int userId, String dateText) {
        LocalDate date = InputValidator.requireDate(dateText, dateText != null && !dateText.isBlank());
        List<TransactionDTO> items = transactionDAO.listBetween(userId, date, date);
        return Map.of("items", items);
    }

    private TransactionDTO read(JsonObject data, Integer transactionId) {
        InputValidator.requireCategory(JsonFields.integer(data, "categoryId"));
        int categoryId = JsonFields.integer(data, "categoryId");
        CategoryDTO category = categoryDAO.findById(categoryId);
        if (category == null) {
            throw new BizException(ErrorCode.E103, "카테고리를 찾을 수 없습니다");
        }
        int amount = InputValidator.requireAmount(JsonFields.integer(data, "amount"), JsonFields.present(data, "amount"));
        LocalDate date = InputValidator.requireDate(JsonFields.text(data, "transactionDate"),
                JsonFields.present(data, "transactionDate"));
        String fixed = InputValidator.fixedTypeFor(category.getType(), JsonFields.text(data, "fixedType"),
                JsonFields.present(data, "fixedType"));
        if ("INCOME".equals(category.getType()) && JsonFields.present(data, "fixedType")) {
            ServerLogger.warn(Thread.currentThread().getName(), "TX_ADD", 0, "수입 내역의 fixedType 은 무시합니다");
        }
        String repeat = InputValidator.repeatOrNone(JsonFields.text(data, "repeatCycle"),
                JsonFields.present(data, "repeatCycle"));
        String memo = InputValidator.memoOrNull(JsonFields.text(data, "memo"));
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(transactionId);
        dto.setCategoryId(categoryId);
        dto.setAmount(amount);
        dto.setTransactionDate(date);
        dto.setFixedType(fixed);
        dto.setRepeatCycle(repeat);
        dto.setMemo(memo);
        return dto;
    }

    private void requireOwner(int userId, int transactionId) {
        Integer owner = transactionDAO.ownerOf(transactionId);
        if (owner == null) {
            throw new BizException(ErrorCode.E301);
        }
        if (owner != userId) {
            throw new BizException(ErrorCode.E302);
        }
    }
}
