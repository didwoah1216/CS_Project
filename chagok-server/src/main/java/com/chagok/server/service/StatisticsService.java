package com.chagok.server.service;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.WarningPolicy;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final TransactionDAO transactionDAO;
    private final CategoryDAO categoryDAO;

    public StatisticsService(TransactionDAO transactionDAO, CategoryDAO categoryDAO) {
        this.transactionDAO = transactionDAO;
        this.categoryDAO = categoryDAO;
    }

    public Map<String, Object> categoryStat(int userId, String ym, String type) {
        String kind = type == null || type.isBlank() ? "EXPENSE" : type.trim();
        if (!"EXPENSE".equals(kind) && !"INCOME".equals(kind)) {
            throw new com.chagok.common.protocol.BizException(
                    com.chagok.common.protocol.ErrorCode.E103, "통계 구분은 INCOME 또는 EXPENSE 여야 합니다");
        }
        LocalDate[] range = DateUtil.monthRange(ym);
        Map<Integer, Long> sums = transactionDAO.sumByCategory(userId, kind, range[0], range[1]);
        List<CategoryStatDTO> items = new ArrayList<>();
        long total = 0;
        for (CategoryDTO category : categoryDAO.findByType(kind)) {
            long amount = sums.getOrDefault(category.getCategoryId(), 0L);
            if (amount <= 0) {
                continue;
            }
            CategoryStatDTO item = new CategoryStatDTO();
            item.setCategoryId(category.getCategoryId());
            item.setName(category.getName());
            item.setAmount(amount);
            items.add(item);
            total += amount;
        }
        items.sort(Comparator.comparingLong(CategoryStatDTO::getAmount).reversed());
        for (CategoryStatDTO item : items) {
            item.setRatio(total == 0 ? 0 : WarningPolicy.round1(item.getAmount() * 100.0 / total));
        }
        if (items.size() > 5) {
            long etc = 0;
            for (int i = 5; i < items.size(); i++) {
                etc += items.get(i).getAmount();
            }
            items = new ArrayList<>(items.subList(0, 5));
            CategoryStatDTO other = new CategoryStatDTO();
            other.setCategoryId(0);
            other.setName("기타");
            other.setAmount(etc);
            other.setRatio(WarningPolicy.round1(etc * 100.0 / total));
            items.add(other);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", total);
        return body;
    }

    public Map<String, Object> monthly(int userId, String ym) {
        DateUtil.parseYm(ym);
        MonthlyStatDTO current = transactionDAO.monthlySummary(userId, ym);
        MonthlyStatDTO previous = transactionDAO.monthlySummary(userId, DateUtil.previousYm(ym));
        long diff = current.getExpense() - previous.getExpense();
        Double rate = previous.getExpense() == 0 ? null : WarningPolicy.round1(diff * 100.0 / previous.getExpense());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("current", current);
        body.put("previous", previous);
        body.put("diffAmount", diff);
        body.put("diffRate", rate);
        return body;
    }
}
