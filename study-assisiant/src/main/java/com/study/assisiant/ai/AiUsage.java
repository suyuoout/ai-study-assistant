package com.study.assisiant.ai;

import java.time.LocalDateTime;

/**
 * 一次 AI 调用的用量记录（ai_usage）。
 */
public class AiUsage {

    private Long id;
    private Long userId;
    /** CHAT / RAG / PLAN / REPORT / FLASHCARD */
    private String kind;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Boolean success;
    /** 是否使用用户自己的 Key（不计入系统额度） */
    private Boolean ownKey;
    private LocalDateTime createTime;

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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getOwnKey() {
        return ownKey;
    }

    public void setOwnKey(Boolean ownKey) {
        this.ownKey = ownKey;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
