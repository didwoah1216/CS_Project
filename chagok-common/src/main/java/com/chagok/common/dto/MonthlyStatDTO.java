package com.chagok.common.dto;

public class MonthlyStatDTO {
    private String ym;
    private long income;
    private long fixed;
    private long variable;
    private long expense;

    public MonthlyStatDTO() {
    }

    public MonthlyStatDTO(String ym, long income, long fixed, long variable) {
        this.ym = ym;
        this.income = income;
        this.fixed = fixed;
        this.variable = variable;
        this.expense = fixed + variable;
    }

    public String getYm() {
        return ym;
    }

    public void setYm(String ym) {
        this.ym = ym;
    }

    public long getIncome() {
        return income;
    }

    public void setIncome(long income) {
        this.income = income;
    }

    public long getFixed() {
        return fixed;
    }

    public void setFixed(long fixed) {
        this.fixed = fixed;
    }

    public long getVariable() {
        return variable;
    }

    public void setVariable(long variable) {
        this.variable = variable;
    }

    public long getExpense() {
        return expense;
    }

    public void setExpense(long expense) {
        this.expense = expense;
    }
}
