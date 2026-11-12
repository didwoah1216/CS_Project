package com.chagok.common.dto;

public class SettingDTO {
    private boolean warnEnabled = true;
    private int warnRatio = 70;
    private int peerGapRatio = 10;
    private int budgetAlertRatio = 80;
    private boolean compareSameGender;

    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    public void setWarnEnabled(boolean warnEnabled) {
        this.warnEnabled = warnEnabled;
    }

    public int getWarnRatio() {
        return warnRatio;
    }

    public void setWarnRatio(int warnRatio) {
        this.warnRatio = warnRatio;
    }

    public int getPeerGapRatio() {
        return peerGapRatio;
    }

    public void setPeerGapRatio(int peerGapRatio) {
        this.peerGapRatio = peerGapRatio;
    }

    public int getBudgetAlertRatio() {
        return budgetAlertRatio;
    }

    public void setBudgetAlertRatio(int budgetAlertRatio) {
        this.budgetAlertRatio = budgetAlertRatio;
    }

    public boolean isCompareSameGender() {
        return compareSameGender;
    }

    public void setCompareSameGender(boolean compareSameGender) {
        this.compareSameGender = compareSameGender;
    }
}
