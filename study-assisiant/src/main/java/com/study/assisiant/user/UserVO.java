package com.study.assisiant.user;

import java.time.LocalDateTime;

/**
 * 对外返回的用户信息（永不含密码、永不含明文 Key）。
 */
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private Boolean enabled;
    private Boolean mustChangePassword;
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

    public static UserVO of(SysUser user, String maskedKey) {
        UserVO vo = new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getCreateTime()
        );
        vo.setRole(user.getRole());
        vo.setEnabled(user.getEnabled() == null || user.getEnabled());
        vo.setMustChangePassword(Boolean.TRUE.equals(
                user.getMustChangePassword()));
        vo.setAiKeyMasked(maskedKey);
        return vo;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
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
