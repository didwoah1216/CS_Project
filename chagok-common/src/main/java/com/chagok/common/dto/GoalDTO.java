package com.chagok.common.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** FR-15 설계용 전송 객체. 저축 목표는 3단계 범위다. */
public class GoalDTO {
    private Integer goalId;
    private String goalName;
    private int targetAmount;
    private int savedAmount;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String status;
    private double progress;
    private long monthlyNeed;
    private int dDay;
    private List<DepositDTO> deposits = new ArrayList<>();

    public Integer getGoalId() {
        return goalId;
    }

    public void setGoalId(Integer goalId) {
        this.goalId = goalId;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public int getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(int savedAmount) {
        this.savedAmount = savedAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public long getMonthlyNeed() {
        return monthlyNeed;
    }

    public void setMonthlyNeed(long monthlyNeed) {
        this.monthlyNeed = monthlyNeed;
    }

    public int getDDay() {
        return dDay;
    }

    public void setDDay(int dDay) {
        this.dDay = dDay;
    }

    public List<DepositDTO> getDeposits() {
        return deposits;
    }

    public void setDeposits(List<DepositDTO> deposits) {
        this.deposits = deposits == null ? new ArrayList<>() : deposits;
    }

    public static class DepositDTO {
        private Integer depositId;
        private int amount;
        private LocalDate depositDate;
        private String memo;

        public Integer getDepositId() {
            return depositId;
        }

        public void setDepositId(Integer depositId) {
            this.depositId = depositId;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public LocalDate getDepositDate() {
            return depositDate;
        }

        public void setDepositDate(LocalDate depositDate) {
            this.depositDate = depositDate;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }
    }
}
