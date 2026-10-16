package com.study.assisiant.auth;

import com.study.assisiant.user.UserVO;

public record LoginResponse(UserVO user, String token, long expiresInSeconds) {
}
