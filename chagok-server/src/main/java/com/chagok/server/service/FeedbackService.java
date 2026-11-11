package com.chagok.server.service;

import com.chagok.common.dto.CategoryStatDTO;
import com.chagok.common.dto.MonthlyStatDTO;
import com.chagok.common.dto.SettingDTO;
import com.chagok.common.dto.WarningDTO;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.WarningPolicy;
import com.chagok.server.ClientSession;
import com.chagok.server.dao.TransactionDAO;
import com.chagok.server.dao.UserSettingDAO;

import java.util.ArrayList;
import java.util.List;

public class FeedbackService {
    private final TransactionDAO transactionDAO;
    private final UserSettingDAO userSettingDAO;
    private final CompareService compareService;

    public FeedbackService(TransactionDAO transactionDAO, UserSettingDAO userSettingDAO, CompareService compareService) {
        this.transactionDAO = transactionDAO;
        this.userSettingDAO = userSettingDAO;
        this.compareService = compareService;
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
}
