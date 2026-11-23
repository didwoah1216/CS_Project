package com.chagok.server.service;

import com.chagok.common.dto.BudgetDTO;
import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.SettingDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.BudgetPolicy;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.InputValidator;
import com.chagok.server.dao.BudgetDAO;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.UserSettingDAO;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BudgetService {
    private final BudgetDAO budgetDAO;
    private final CategoryDAO categoryDAO;
    private final UserSettingDAO userSettingDAO;

    public BudgetService(BudgetDAO budgetDAO, CategoryDAO categoryDAO, UserSettingDAO userSettingDAO) {
        this.budgetDAO = budgetDAO;
        this.categoryDAO = categoryDAO;
        this.userSettingDAO = userSettingDAO;
    }

    public Map<String, Object> set(int userId, JsonObject data) {
        if (!JsonFields.present(data, "categoryId")) {
            throw new BizException(ErrorCode.E102, "카테고리를 선택해야 합니다");
        }
        int categoryId = JsonFields.integer(data, "categoryId");
        InputValidator.requireCategory(categoryId);
        CategoryDTO category = categoryDAO.findById(categoryId);
        if (category == null) {
            throw new BizException(ErrorCode.E103, "카테고리를 찾을 수 없습니다");
        }
        if (!"EXPENSE".equals(category.getType())) {
            throw new BizException(ErrorCode.E304, "예산은 지출 카테고리만 설정할 수 있습니다");
        }
        int amount = InputValidator.requireAmount(JsonFields.integer(data, "amount"), JsonFields.present(data, "amount"));
        String ym = requiredYm(data);
        budgetDAO.upsert(userId, categoryId, ym, amount);
        BudgetDTO saved = budgetDAO.findByCategoryMonth(userId, categoryId, ym);
        apply(saved, alertRatio(userId));
        return Map.of("budget", saved);
    }

    public Map<String, Object> list(int userId, String ym) {
        DateUtil.parseYm(ym);
        int alert = alertRatio(userId);
        List<BudgetDTO> items = budgetDAO.findByMonth(userId, ym);
        for (BudgetDTO item : items) {
            apply(item, alert);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("alertRatio", alert);
        return body;
    }

    public Map<String, Object> delete(int userId, JsonObject data) {
        if (!JsonFields.present(data, "budgetId")) {
            throw new BizException(ErrorCode.E102, "예산 번호가 필요합니다");
        }
        int budgetId = JsonFields.integer(data, "budgetId");
        Integer owner = budgetDAO.ownerOf(budgetId);
        if (owner == null) {
            throw new BizException(ErrorCode.E301, "예산이 없습니다");
        }
        if (owner != userId) {
            throw new BizException(ErrorCode.E302, "다른 사용자의 예산입니다");
        }
        budgetDAO.delete(userId, budgetId);
        return Map.of("deleted", true);
    }

    public Map<String, Object> copy(int userId, String ym) {
        DateUtil.parseYm(ym);
        String from = DateUtil.previousYm(ym);
        int count = budgetDAO.countMonth(userId, from);
        if (count == 0) {
            throw new BizException(ErrorCode.E301, "전월 예산이 없습니다");
        }
        budgetDAO.copyMonth(userId, from, ym);
        Map<String, Object> body = list(userId, ym);
        body.put("copied", count);
        return body;
    }

    private int alertRatio(int userId) {
        SettingDTO setting = userSettingDAO.findByUser(userId);
        if (setting == null) {
            return new SettingDTO().getBudgetAlertRatio();
        }
        return setting.getBudgetAlertRatio();
    }

    private static void apply(BudgetDTO dto, int alertRatio) {
        double rate = BudgetPolicy.usageRate(dto.getUsed(), dto.getAmount());
        dto.setUsageRate(rate);
        dto.setStatus(BudgetPolicy.status(rate, alertRatio));
    }

    private static String requiredYm(JsonObject data) {
        String ym = JsonFields.text(data, "ym");
        if (ym == null || ym.isBlank()) {
            throw new BizException(ErrorCode.E102, "월(ym)이 필요합니다");
        }
        DateUtil.parseYm(ym.trim());
        return ym.trim();
    }
}
