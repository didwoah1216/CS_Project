package com.chagok.common.dto;

public class CategoryDTO {
    private int categoryId;
    private String type;
    private String name;
    private String defaultFixedType;

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultFixedType() {
        return defaultFixedType;
    }

    public void setDefaultFixedType(String defaultFixedType) {
        this.defaultFixedType = defaultFixedType;
    }
}
