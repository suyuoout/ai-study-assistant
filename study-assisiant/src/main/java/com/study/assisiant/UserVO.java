
package com.study.assisiant;

import java.time.LocalDateTime;

public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String aiKeyMasked;
    private LocalDateTime createTime;

    public UserVO() {
    }

    public UserVO(Long id, String username, String nickname,
                  String email, LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAiKeyMasked() {
        return aiKeyMasked;
    }

    public void setAiKeyMasked(String aiKeyMasked) {
        this.aiKeyMasked = aiKeyMasked;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
