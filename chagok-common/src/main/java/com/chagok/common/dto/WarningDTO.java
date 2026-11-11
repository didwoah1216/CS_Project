package com.chagok.common.dto;

import java.util.ArrayList;
import java.util.List;

public class WarningDTO {
    private String level;
    private Double ratio;
    private int warnRatio;
    private Double peerAvgRatio;
    private long available;
    private long variable;
    private String message;
    private List<CategoryStatDTO> topCategories = new ArrayList<>();
    private boolean showPopup;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Double getRatio() {
        return ratio;
    }

    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    public int getWarnRatio() {
        return warnRatio;
    }

    public void setWarnRatio(int warnRatio) {
        this.warnRatio = warnRatio;
    }

    public Double getPeerAvgRatio() {
        return peerAvgRatio;
    }

    public void setPeerAvgRatio(Double peerAvgRatio) {
        this.peerAvgRatio = peerAvgRatio;
    }

    public long getAvailable() {
        return available;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    public long getVariable() {
        return variable;
    }

    public void setVariable(long variable) {
        this.variable = variable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<CategoryStatDTO> getTopCategories() {
        return topCategories;
    }

    public void setTopCategories(List<CategoryStatDTO> topCategories) {
        this.topCategories = topCategories == null ? new ArrayList<>() : topCategories;
    }

    public boolean isShowPopup() {
        return showPopup;
    }

    public void setShowPopup(boolean showPopup) {
        this.showPopup = showPopup;
    }
}
