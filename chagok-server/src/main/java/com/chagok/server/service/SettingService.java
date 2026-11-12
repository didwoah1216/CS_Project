package com.chagok.server.service;

import com.chagok.common.dto.SettingDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.server.dao.UserSettingDAO;
import com.google.gson.JsonObject;

import java.util.Map;

public class SettingService {
    private final UserSettingDAO userSettingDAO;

    public SettingService(UserSettingDAO userSettingDAO) {
        this.userSettingDAO = userSettingDAO;
    }

    public Map<String, Object> get(int userId) {
        SettingDTO setting = userSettingDAO.findByUser(userId);
        if (setting == null) {
            setting = new SettingDTO();
        }
        return Map.of("setting", setting);
    }

    public Map<String, Object> update(int userId, JsonObject data) {
        SettingDTO setting = new SettingDTO();
        setting.setWarnEnabled(JsonFields.bool(data, "warnEnabled", true));
        Integer warnRatio = JsonFields.integer(data, "warnRatio");
        Integer peerGap = JsonFields.integer(data, "peerGapRatio");
        Integer budgetAlert = JsonFields.integer(data, "budgetAlertRatio");
        if (!JsonFields.present(data, "warnRatio") || !JsonFields.present(data, "peerGapRatio")
                || !JsonFields.present(data, "budgetAlertRatio") || !JsonFields.present(data, "compareSameGender")) {
            throw new BizException(ErrorCode.E102, "설정 값이 모두 필요합니다");
        }
        if (warnRatio < 10 || warnRatio > 100 || peerGap < 0 || peerGap > 50 || budgetAlert < 50 || budgetAlert > 100) {
            throw new BizException(ErrorCode.E103, "설정 값 범위를 벗어났습니다");
        }
        setting.setWarnRatio(warnRatio);
        setting.setPeerGapRatio(peerGap);
        setting.setBudgetAlertRatio(budgetAlert);
        setting.setCompareSameGender(JsonFields.bool(data, "compareSameGender", true));
        userSettingDAO.update(userId, setting);
        return Map.of("setting", setting);
    }
}
