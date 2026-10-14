package com.study.assisiant.ai;


public class AIRequest {


    /**
     * 学习目标
     */
    private String goal;


    /**
     * 当前水平
     */
    private String level;


    /**
     * 学习周期
     */
    private Integer days;


    /**
     * 每日学习时间
     */
    private Integer hours;


    public String getGoal() {
        return goal;
    }


    public void setGoal(String goal) {
        this.goal = goal;
    }


    public String getLevel() {
        return level;
    }


    public void setLevel(String level) {
        this.level = level;
    }


    public Integer getDays() {
        return days;
    }


    public void setDays(Integer days) {
        this.days = days;
    }


    public Integer getHours() {
        return hours;
    }


    public void setHours(Integer hours) {
        this.hours = hours;
    }
}