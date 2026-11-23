package com.chagok.server.service;

import com.chagok.common.dto.BudgetDTO;
import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.ScoreDTO;
import com.chagok.common.dto.SettingDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.util.BudgetPolicy;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.ScorePolicy;
import com.chagok.common.util.WarningPolicy;
import com.chagok.server.ClientSession;
import com.chagok.server.dao.BudgetDAO;
import com.chagok.server.dao.HabitScoreDAO;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserSettingDAO;

import java.util.ArrayList;
import java.util.List;

public class FeedbackService {
    private final TransactionDAO transactionDAO;
    private final UserSettingDAO userSettingDAO;
    private final CompareService compareService;
    private final BudgetDAO budgetDAO;
    private final HabitScoreDAO habitScoreDAO;

    public FeedbackService(TransactionDAO transactionDAO, UserSettingDAO userSettingDAO, CompareService compareService,
                           BudgetDAO budgetDAO, HabitScoreDAO habitScoreDAO) {
        this.transactionDAO = transactionDAO;
        this.userSettingDAO = userSettingDAO;
        this.compareService = compareService;
        this.budgetDAO = budgetDAO;
        this.habitScoreDAO = habitScoreDAO;
    }

    public WarningDTO evaluate(ClientSession session, String ym) {
        DateUtil.parseYm(ym);
        int userId = session.getUserId();
        SettingDTO setting = userSettingDAO.findByUser(userId);
        if (setting == null) {
            setting = new SettingDTO();
        }
        MonthlyStatDTO month = transactionDAO.monthlySummary(userId, ym);
        Boolean sameGender = setting.isCompareSameGender();
        Double peer = compareService.peerAvgRatio(userId, ym, sameGender);
        List<CategoryStatDTO> top = compareService.overPeerTop2(userId, ym, sameGender);
        List<String> names = new ArrayList<>();
        for (CategoryStatDTO item : top) {
            names.add(item.getName());
        }
        WarningPolicy.Decision decision = WarningPolicy.decide(
                month.getIncome(), month.getFixed(), month.getVariable(),
                setting.getWarnRatio(), setting.getPeerGapRatio(), peer, names);
        WarningDTO dto = new WarningDTO();
        dto.setLevel(decision.level.name());
        dto.setRatio(decision.ratio);
        dto.setWarnRatio(setting.getWarnRatio());
        dto.setPeerAvgRatio(peer);
        dto.setAvailable(decision.available);
        dto.setVariable(month.getVariable());
        dto.setMessage(decision.message);
        dto.setTopCategories(top);
        boolean popup = setting.isWarnEnabled()
                && decision.level.atLeast(WarningPolicy.Level.WARNING)
                && session.markPopup(ym);
        dto.setShowPopup(popup);
        return dto;
    }

    public ScoreDTO score(int userId, String ym, boolean save) {
        DateUtil.parseYm(ym);
        MonthlyStatDTO month = transactionDAO.monthlySummary(userId, ym);
        Double ratio = flexRatio(month);
        double usedRatio = ratio == null ? 100.0 : ratio;
        int flex = ScorePolicy.flexPoints(usedRatio);
        List<BudgetDTO> budgets = budgetDAO.findByMonth(userId, ym);
        int kept = 0;
        for (BudgetDTO budget : budgets) {
            if (BudgetPolicy.kept(budget.getUsed(), budget.getAmount())) {
                kept++;
            }
        }
        int budget = ScorePolicy.budgetPoints(kept, budgets.size());
        String previousYm = DateUtil.previousYm(ym);
        MonthlyStatDTO previous = transactionDAO.monthlySummary(userId, previousYm);
        boolean hasPrevious = previous.getIncome() != 0 || previous.getFixed() != 0 || previous.getVariable() != 0;
        int change = ScorePolicy.changePoints(ScorePolicy.changeRate(hasPrevious, previous.getVariable(), month.getVariable()));
        int peer = ScorePolicy.peerPoints(usedRatio, compareService.peerAvgRatio(userId, ym, null));
        int total = flex + budget + change + peer;
        ScoreDTO dto = new ScoreDTO();
        dto.setYm(ym);
        dto.setScore(total);
        dto.setGrade(ScorePolicy.grade(total));
        dto.setFlexRatio(ratio == null ? null : usedRatio);
        dto.setItems(List.of(
                item("유동비율", flex, 40),
                item("예산 준수", budget, 25),
                item("전월 대비", change, 20),
                item("또래 대비", peer, 15)));
        dto.setMessage(ScorePolicy.message(usedRatio, flex, budget, budgets.size(), peer));
        dto.setPrevScore(habitScoreDAO.findScore(userId, previousYm));
        if (save) {
            habitScoreDAO.upsert(userId, ym, total, dto.getGrade(), usedRatio, dto.getMessage());
        }
        return dto;
    }

    private static ScoreDTO.ScoreItem item(String name, int point, int max) {
        ScoreDTO.ScoreItem item = new ScoreDTO.ScoreItem();
        item.setName(name);
        item.setPoint(point);
        item.setMax(max);
        return item;
    }

    private static Double flexRatio(MonthlyStatDTO month) {
        long available = month.getIncome() - month.getFixed();
        if (month.getIncome() == 0 || available <= 0) {
            return null;
        }
        return WarningPolicy.round1(month.getVariable() * 100.0 / available);
    }
}
