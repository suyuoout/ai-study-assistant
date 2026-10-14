package com.study.assisiant;


public class LoginResponse {

    private UserVO user;

    private String token;


    public LoginResponse(UserVO user, String token) {
        this.user = user;
        this.token = token;
    }


    public UserVO getUser() {
        return user;
    }


    public void setUser(UserVO user) {
        this.user = user;
    }


    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }
}