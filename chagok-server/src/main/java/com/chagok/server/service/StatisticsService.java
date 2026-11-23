package com.chagok.server.service;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.WeekdayStatDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.InputValidator;
import com.chagok.common.util.WarningPolicy;
import com.chagok.common.util.WeekdayPolicy;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.YearMonth;
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

    public Map<String, Object> trend(int userId, String ym) {
        YearMonth end = DateUtil.parseYm(ym);
        List<MonthlyStatDTO> items = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            items.add(transactionDAO.monthlySummary(userId, end.minusMonths(i).toString()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        return body;
    }

    public Map<String, Object> weekday(int userId, JsonObject data) {
        LocalDate[] range = range(data);
        Map<Integer, long[]> rows = transactionDAO.weekdayVariable(userId, range[0], range[1]);
        long total = 0;
        for (long[] row : rows.values()) {
            total += row[0];
        }
        List<WeekdayStatDTO> items = new ArrayList<>();
        long weekend = 0;
        WeekdayStatDTO top = null;
        for (int dow = 1; dow <= 7; dow++) {
            long[] row = rows.getOrDefault(dow, new long[]{0, 0});
            WeekdayStatDTO item = new WeekdayStatDTO();
            item.setDow(dow);
            item.setName(WeekdayPolicy.name(dow));
            item.setAmount(row[0]);
            item.setCount((int) row[1]);
            item.setRatio(total == 0 ? 0 : WarningPolicy.round1(row[0] * 100.0 / total));
            items.add(item);
            if (WeekdayPolicy.weekend(dow)) {
                weekend += row[0];
            }
            if (top == null || item.getAmount() > top.getAmount()) {
                top = item;
            }
        }
        double weekendRatio = total == 0 ? 0 : WarningPolicy.round1(weekend * 100.0 / total);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", total);
        body.put("weekendAmount", weekend);
        body.put("weekendRatio", weekendRatio);
        body.put("concentrated", WeekdayPolicy.concentrated(weekendRatio) && total > 0);
        body.put("message", WeekdayPolicy.message(total, top == null ? "" : top.getName(),
                top == null ? 0 : top.getRatio(), weekendRatio));
        return body;
    }

    private static LocalDate[] range(JsonObject data) {
        boolean hasFrom = JsonFields.present(data, "from") && !JsonFields.text(data, "from").isBlank();
        boolean hasTo = JsonFields.present(data, "to") && !JsonFields.text(data, "to").isBlank();
        if (hasFrom || hasTo) {
            if (!hasFrom || !hasTo) {
                throw new BizException(ErrorCode.E102, "시작일과 종료일을 함께 입력해야 합니다");
            }
            LocalDate from = InputValidator.requireDate(JsonFields.text(data, "from"), true);
            LocalDate to = InputValidator.requireDate(JsonFields.text(data, "to"), true);
            if (from.isAfter(to)) {
                throw new BizException(ErrorCode.E103, "시작일이 종료일보다 늦습니다");
            }
            return new LocalDate[]{from, to};
        }
        if (!JsonFields.present(data, "ym") || JsonFields.text(data, "ym").isBlank()) {
            throw new BizException(ErrorCode.E102, "월(ym) 또는 기간이 필요합니다");
        }
        String ym = JsonFields.text(data, "ym").trim();
        return DateUtil.monthRange(ym);
    }
}
