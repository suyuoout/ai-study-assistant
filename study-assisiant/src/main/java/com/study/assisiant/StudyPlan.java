package com.study.assisiant;


import java.time.LocalDate;
import java.time.LocalDateTime;


public class StudyPlan {


    private Long id;


    private Long userId;


    private String title;

    private String goal;


    private String content;


    private LocalDate startDate;


    private LocalDate endDate;

    private Integer dailyMinutes;


    private String status;


    private LocalDateTime createTime;


    private LocalDateTime updateTime;



    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Long getUserId() {
        return userId;
    }


    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }


    public String getContent() {
        return content;
    }


    public void setContent(String content) {
        this.content = content;
    }


    public LocalDate getStartDate() {
        return startDate;
    }


    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }


    public LocalDate getEndDate() {
        return endDate;
    }


    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getDailyMinutes() {
        return dailyMinutes;
    }

    public void setDailyMinutes(Integer dailyMinutes) {
        this.dailyMinutes = dailyMinutes;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDateTime getCreateTime() {
        return createTime;
    }


    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }


    public LocalDateTime getUpdateTime() {
        return updateTime;
    }


    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
