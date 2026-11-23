package com.chagok.common.dto;

/** FR-16 요일별 유동지출. dow 는 MySQL DAYOFWEEK 와 같이 1=일 ~ 7=토. */
public class WeekdayStatDTO {
    private int dow;
    private String name;
    private long amount;
    private int count;
    private double ratio;

    public int getDow() {
        return dow;
    }

    public void setDow(int dow) {
        this.dow = dow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}
