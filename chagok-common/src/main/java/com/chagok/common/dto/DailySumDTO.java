package com.chagok.common.dto;

public class DailySumDTO {
    private String date;
    private long income;
    private long expense;

    public DailySumDTO() {
    }

    public DailySumDTO(String date, long income, long expense) {
        this.date = date;
        this.income = income;
        this.expense = expense;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getIncome() {
        return income;
    }

    public void setIncome(long income) {
        this.income = income;
    }

    public long getExpense() {
        return expense;
    }

    public void setExpense(long expense) {
        this.expense = expense;
    }
}
