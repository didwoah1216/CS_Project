package com.chagok.common.dto;

public class CategoryStatDTO {
    private int categoryId;
    private String name;
    private long amount;
    private Double ratio;
    private Long peerAvg;
    private Long diff;
    private Double diffRate;

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
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

    public Double getRatio() {
        return ratio;
    }

    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    public Long getPeerAvg() {
        return peerAvg;
    }

    public void setPeerAvg(Long peerAvg) {
        this.peerAvg = peerAvg;
    }

    public Long getDiff() {
        return diff;
    }

    public void setDiff(Long diff) {
        this.diff = diff;
    }

    public Double getDiffRate() {
        return diffRate;
    }

    public void setDiffRate(Double diffRate) {
        this.diffRate = diffRate;
    }
}
