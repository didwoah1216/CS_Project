package com.chagok.common.dto;

import java.util.ArrayList;
import java.util.List;

public class CompareDTO {
    private int ageGroup;
    private boolean sameGender;
    private int sampleSize;
    private List<CategoryStatDTO> items = new ArrayList<>();
    private long myTotal;
    private long peerTotal;
    private Double myRatio;
    private Double peerRatio;

    public int getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(int ageGroup) {
        this.ageGroup = ageGroup;
    }

    public boolean isSameGender() {
        return sameGender;
    }

    public void setSameGender(boolean sameGender) {
        this.sameGender = sameGender;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public List<CategoryStatDTO> getItems() {
        return items;
    }

    public void setItems(List<CategoryStatDTO> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public long getMyTotal() {
        return myTotal;
    }

    public void setMyTotal(long myTotal) {
        this.myTotal = myTotal;
    }

    public long getPeerTotal() {
        return peerTotal;
    }

    public void setPeerTotal(long peerTotal) {
        this.peerTotal = peerTotal;
    }

    public Double getMyRatio() {
        return myRatio;
    }

    public void setMyRatio(Double myRatio) {
        this.myRatio = myRatio;
    }

    public Double getPeerRatio() {
        return peerRatio;
    }

    public void setPeerRatio(Double peerRatio) {
        this.peerRatio = peerRatio;
    }
}
