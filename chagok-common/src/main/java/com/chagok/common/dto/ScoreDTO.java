package com.chagok.common.dto;

import java.util.ArrayList;
import java.util.List;

/** FR-14 설계용 전송 객체. 점수 계산은 2단계 범위다. */
public class ScoreDTO {
    private String ym;
    private int score;
    private String grade;
    private List<ScoreItem> items = new ArrayList<>();
    private String message;
    private Integer prevScore;

    public String getYm() {
        return ym;
    }

    public void setYm(String ym) {
        this.ym = ym;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public List<ScoreItem> getItems() {
        return items;
    }

    public void setItems(List<ScoreItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPrevScore() {
        return prevScore;
    }

    public void setPrevScore(Integer prevScore) {
        this.prevScore = prevScore;
    }

    public static class ScoreItem {
        private String name;
        private int point;
        private int max;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPoint() {
            return point;
        }

        public void setPoint(int point) {
            this.point = point;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }
}
