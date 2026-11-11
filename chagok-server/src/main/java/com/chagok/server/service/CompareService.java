package com.chagok.server.service;

import com.chagok.common.dto.CategoryDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.CompareDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.SettingDTO;
import com.chagok.common.dto.UserDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.AgeGroup;
import com.chagok.common.util.ComparePolicy;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.WarningPolicy;
import com.chagok.server.dao.CategoryDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserDAO;
import com.chagok.server.dao.UserSettingDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CompareService {
    private final UserDAO userDAO;
    private final CategoryDAO categoryDAO;
    private final TransactionDAO transactionDAO;
    private final UserSettingDAO userSettingDAO;

    public CompareService(UserDAO userDAO, CategoryDAO categoryDAO, TransactionDAO transactionDAO,
                          UserSettingDAO userSettingDAO) {
        this.userDAO = userDAO;
        this.categoryDAO = categoryDAO;
        this.transactionDAO = transactionDAO;
        this.userSettingDAO = userSettingDAO;
    }

    public CompareDTO compare(int userId, String ym, Boolean sameGender) {
        DateUtil.parseYm(ym);
        UserDTO me = requireUser(userId);
        String gender = genderFilter(userId, me, sameGender);
        AgeGroup group = AgeGroup.of(me.getAge());
        int sample = userDAO.countPeers(group.from(), group.to(), gender, userId);
        ComparePolicy.requireSample(sample);
        List<CategoryStatDTO> items = categoryItems(userId, ym, group, gender, sample, false);
        long myTotal = 0;
        long peerTotal = 0;
        for (CategoryStatDTO item : items) {
            myTotal += item.getAmount();
            peerTotal += item.getPeerAvg() == null ? 0 : item.getPeerAvg();
        }
        items.sort(Comparator.comparingLong(CategoryStatDTO::getAmount).reversed());
        CompareDTO dto = new CompareDTO();
        dto.setAgeGroup(group.from());
        dto.setSameGender(gender != null);
        dto.setSampleSize(sample);
        dto.setItems(items);
        dto.setMyTotal(myTotal);
        dto.setPeerTotal(peerTotal);
        dto.setMyRatio(ratioOf(transactionDAO.monthlySummary(userId, ym)));
        dto.setPeerRatio(averageRatio(transactionDAO.peerMonthlyRows(ym, group.from(), group.to(), gender, userId)));
        return dto;
    }

    public Double peerAvgRatio(int userId, String ym, Boolean sameGender) {
        UserDTO me = requireUser(userId);
        String gender = genderFilter(userId, me, sameGender);
        AgeGroup group = AgeGroup.of(me.getAge());
        int sample = userDAO.countPeers(group.from(), group.to(), gender, userId);
        if (sample < ComparePolicy.MIN_SAMPLE) {
            return null;
        }
        return averageRatio(transactionDAO.peerMonthlyRows(ym, group.from(), group.to(), gender, userId));
    }

    public List<CategoryStatDTO> overPeerTop2(int userId, String ym, Boolean sameGender) {
        UserDTO me = requireUser(userId);
        String gender = genderFilter(userId, me, sameGender);
        AgeGroup group = AgeGroup.of(me.getAge());
        int sample = userDAO.countPeers(group.from(), group.to(), gender, userId);
        boolean hasPeer = sample >= ComparePolicy.MIN_SAMPLE;
        List<CategoryStatDTO> items = categoryItems(userId, ym, group, gender, Math.max(sample, 1), true);
        if (hasPeer) {
            items.removeIf(item -> item.getDiff() == null || item.getDiff() <= 0);
            items.sort(Comparator.comparingLong(CategoryStatDTO::getDiff).reversed());
        } else {
            items.sort(Comparator.comparingLong(CategoryStatDTO::getAmount).reversed());
        }
        if (items.size() > 2) {
            return new ArrayList<>(items.subList(0, 2));
        }
        return items;
    }

    private List<CategoryStatDTO> categoryItems(int userId, String ym, AgeGroup group, String gender,
                                                int sample, boolean variableOnly) {
        LocalDate[] range = DateUtil.monthRange(ym);
        Map<Integer, Long> mine = transactionDAO.sumByCategory(userId, "EXPENSE", range[0], range[1]);
        Map<Integer, Long> peer = sample >= ComparePolicy.MIN_SAMPLE
                ? transactionDAO.peerCategorySum(group.from(), group.to(), gender, userId, range[0], range[1])
                : Map.of();
        List<CategoryStatDTO> items = new ArrayList<>();
        for (CategoryDTO category : categoryDAO.findByType("EXPENSE")) {
            if (variableOnly && !"VARIABLE".equals(category.getDefaultFixedType())) {
                continue;
            }
            long my = mine.getOrDefault(category.getCategoryId(), 0L);
            Long avg = null;
            Long diff = null;
            Double rate = null;
            if (sample >= ComparePolicy.MIN_SAMPLE) {
                avg = Math.round(peer.getOrDefault(category.getCategoryId(), 0L) / (double) sample);
                diff = my - avg;
                rate = avg == 0 ? null : WarningPolicy.round1(diff * 100.0 / avg);
            }
            CategoryStatDTO item = new CategoryStatDTO();
            item.setCategoryId(category.getCategoryId());
            item.setName(category.getName());
            item.setAmount(my);
            item.setPeerAvg(avg);
            item.setDiff(diff);
            item.setDiffRate(rate);
            items.add(item);
        }
        return items;
    }

    private String genderFilter(int userId, UserDTO me, Boolean sameGender) {
        boolean same;
        if (sameGender != null) {
            same = sameGender;
        } else {
            SettingDTO setting = userSettingDAO.findByUser(userId);
            same = setting != null && setting.isCompareSameGender();
        }
        return same ? me.getGender() : null;
    }

    private UserDTO requireUser(int userId) {
        UserDTO user = userDAO.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.E301, "사용자를 찾을 수 없습니다");
        }
        return user;
    }

    private static Double ratioOf(MonthlyStatDTO month) {
        long available = month.getIncome() - month.getFixed();
        if (month.getIncome() == 0 || available <= 0) {
            return null;
        }
        return WarningPolicy.round1(month.getVariable() * 100.0 / available);
    }

    private static Double averageRatio(List<long[]> rows) {
        double sum = 0;
        int count = 0;
        for (long[] row : rows) {
            long income = row[0];
            long available = income - row[1];
            if (income == 0 || available <= 0) {
                continue;
            }
            sum += row[2] * 100.0 / available;
            count++;
        }
        if (count == 0) {
            return null;
        }
        return WarningPolicy.round1(sum / count);
    }
}
