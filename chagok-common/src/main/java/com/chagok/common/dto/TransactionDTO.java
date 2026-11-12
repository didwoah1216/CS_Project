package com.chagok.common.dto;

import java.time.LocalDate;

public class TransactionDTO {
    private Integer transactionId;
    private int categoryId;
    private String categoryName;
    private String type;
    private int amount;
    private LocalDate transactionDate;
    private String fixedType;
    private String repeatCycle;
    private String memo;
    private Integer parentTxId;
    private boolean auto;

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getFixedType() {
        return fixedType;
    }

    public void setFixedType(String fixedType) {
        this.fixedType = fixedType;
    }

    public String getRepeatCycle() {
        return repeatCycle;
    }

    public void setRepeatCycle(String repeatCycle) {
        this.repeatCycle = repeatCycle;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Integer getParentTxId() {
        return parentTxId;
    }

    public void setParentTxId(Integer parentTxId) {
        this.parentTxId = parentTxId;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }
}
