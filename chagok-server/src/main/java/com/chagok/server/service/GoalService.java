package com.chagok.server.service;

import com.chagok.common.dto.GoalDTO;
import com.chagok.common.protocol.BizException;
import com.chagok.common.protocol.ErrorCode;
import com.chagok.common.util.DateUtil;
import com.chagok.common.util.GoalPolicy;
import com.chagok.common.util.InputValidator;
import com.chagok.server.dao.SavingGoalDAO;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoalService {
    private final SavingGoalDAO savingGoalDAO;

    public GoalService(SavingGoalDAO savingGoalDAO) {
        this.savingGoalDAO = savingGoalDAO;
    }

    public Map<String, Object> add(int userId, JsonObject data) {
        String name = GoalPolicy.name(JsonFields.text(data, "goalName"), JsonFields.present(data, "goalName"));
        int target = GoalPolicy.targetAmount(JsonFields.integer(data, "targetAmount"),
                JsonFields.present(data, "targetAmount"));
        LocalDate start = InputValidator.requireDate(JsonFields.text(data, "startDate"),
                JsonFields.present(data, "startDate"));
        LocalDate end = InputValidator.requireDate(JsonFields.text(data, "targetDate"),
                JsonFields.present(data, "targetDate"));
        GoalPolicy.requireTargetNotBeforeStart(start, end);
        if (savingGoalDAO.countActive(userId) >= GoalPolicy.ACTIVE_LIMIT) {
            throw new BizException(ErrorCode.E304, "진행 중인 저축 목표는 5개까지입니다");
        }
        int id = savingGoalDAO.insert(userId, name, target, start, end);
        return Map.of("goal", decorate(savingGoalDAO.find(id)));
    }

    public Map<String, Object> list(int userId) {
        List<GoalDTO> items = savingGoalDAO.list(userId);
        for (GoalDTO item : items) {
            decorate(item);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        return body;
    }

    public Map<String, Object> deposit(int userId, JsonObject data) {
        if (!JsonFields.present(data, "goalId")) {
            throw new BizException(ErrorCode.E102, "목표 번호가 필요합니다");
        }
        int goalId = JsonFields.integer(data, "goalId");
        requireOwner(userId, goalId);
        GoalDTO goal = savingGoalDAO.find(goalId);
        if ("CANCELED".equals(goal.getStatus())) {
            throw new BizException(ErrorCode.E304, "취소된 목표에는 입금할 수 없습니다");
        }
        int amount = GoalPolicy.depositAmount(JsonFields.integer(data, "amount"), JsonFields.present(data, "amount"));
        LocalDate date = InputValidator.requireDate(JsonFields.text(data, "depositDate"),
                JsonFields.present(data, "depositDate"));
        String memo = GoalPolicy.memo(JsonFields.text(data, "memo"));
        savingGoalDAO.addDeposit(goalId, amount, date, memo);
        return Map.of("goal", decorate(savingGoalDAO.find(goalId)));
    }

    public Map<String, Object> delete(int userId, JsonObject data) {
        if (!JsonFields.present(data, "goalId")) {
            throw new BizException(ErrorCode.E102, "목표 번호가 필요합니다");
        }
        int goalId = JsonFields.integer(data, "goalId");
        requireOwner(userId, goalId);
        savingGoalDAO.delete(userId, goalId);
        return Map.of("deleted", true);
    }

    private void requireOwner(int userId, int goalId) {
        Integer owner = savingGoalDAO.ownerOf(goalId);
        if (owner == null) {
            throw new BizException(ErrorCode.E301, "저축 목표가 없습니다");
        }
        if (owner != userId) {
            throw new BizException(ErrorCode.E302, "다른 사용자의 저축 목표입니다");
        }
    }

    private static GoalDTO decorate(GoalDTO goal) {
        LocalDate today = DateUtil.today();
        goal.setProgress(GoalPolicy.progress(goal.getSavedAmount(), goal.getTargetAmount()));
        goal.setMonthlyNeed(GoalPolicy.monthlyNeed(
                goal.getSavedAmount(), goal.getTargetAmount(), today, goal.getTargetDate()));
        goal.setDDay(GoalPolicy.dDay(today, goal.getTargetDate()));
        return goal;
    }
}
