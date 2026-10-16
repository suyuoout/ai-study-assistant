package com.study.assisiant.auth;

import com.study.assisiant.ai.AiRateLimiter;
import com.study.assisiant.ai.AiUsageService;
import com.study.assisiant.common.CryptoService;
import com.study.assisiant.common.Result;
import com.study.assisiant.mapper.SysUserMapper;
import com.study.assisiant.user.SysUser;
import com.study.assisiant.user.UserCache;
import com.study.assisiant.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号相关接口：注册、登录、个人资料、改密、个人 AI Key。
 *
 * <p>注意这里没有「列出所有用户」的接口：那是越权风险，
 * 用户列表只在 /api/admin/users（需要管理员）暴露。</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final UserCache userCache;
    private final CryptoService cryptoService;
    private final LoginAttemptService loginAttemptService;
    private final AiUsageService aiUsageService;
    private final AiRateLimiter aiRateLimiter;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Value("${app.allow-register:true}")
    private boolean allowRegister;

    public UserController(SysUserMapper sysUserMapper,
                          UserCache userCache,
                          CryptoService cryptoService,
                          LoginAttemptService loginAttemptService,
                          AiUsageService aiUsageService,
                          AiRateLimiter aiRateLimiter) {
        this.sysUserMapper = sysUserMapper;
        this.userCache = userCache;
        this.cryptoService = cryptoService;
        this.loginAttemptService = loginAttemptService;
        this.aiUsageService = aiUsageService;
        this.aiRateLimiter = aiRateLimiter;
    }

    @PostMapping("/register")
    public Result<UserVO> register(
            @Valid @RequestBody RegisterRequest request) {

        if (!allowRegister) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "当前环境未开放注册");
        }

        String username = request.username().trim();
        if (sysUserMapper.findByUsername(username) != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(blankToNull(request.nickname()));
        user.setEmail(blankToNull(request.email()));
        user.setRole("USER");
        user.setEnabled(true);
        user.setMustChangePassword(false);

        if (sysUserMapper.insert(user) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "注册失败");
        }

        return Result.success("注册成功", toVO(user));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String username = request.username().trim();
        String ip = clientIp(httpRequest);

        loginAttemptService.checkBlocked(username, ip);

        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null
                || !passwordEncoder.matches(
                request.password(), user.getPassword())) {
            loginAttemptService.onFailure(username, ip);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        loginAttemptService.onSuccess(username, ip);

        String token = JwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole());

        return Result.success("登录成功",
                new LoginResponse(toVO(user), token,
                        JwtUtil.getExpireSeconds()));
    }

    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        return Result.success(toVO(currentUser(request)));
    }

    @PutMapping("/me")
    public Result<UserVO> updateMe(
            @Valid @RequestBody UpdateProfileRequest req,
            HttpServletRequest request) {

        Long userId = currentUserId(request);
        sysUserMapper.updateProfile(userId,
                blankToNull(req.nickname()), blankToNull(req.email()));
        userCache.evict(userId);

        return Result.success("修改成功",
                toVO(sysUserMapper.findById(userId)));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            HttpServletRequest request) {

        Long userId = currentUserId(request);
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "账号不存在，请重新登录");
        }

        if (!passwordEncoder.matches(
                req.oldPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordEncoder.matches(
                req.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
        }

        sysUserMapper.updatePassword(userId,
                passwordEncoder.encode(req.newPassword()));
        sysUserMapper.clearMustChangePassword(userId);
        userCache.evict(userId);

        return Result.success("密码修改成功，请重新登录", null);
    }

    @PutMapping("/me/ai-key")
    public Result<Void> updateAiKey(
            @Valid @RequestBody UpdateAiKeyRequest body,
            HttpServletRequest request) {

        Long userId = currentUserId(request);

        String key = body.aiKey() == null ? null : body.aiKey().trim();
        if (key != null && key.isEmpty()) {
            key = null; // 传空表示清除，恢复使用系统默认 Key
        }

        // 加密后落库，数据库里不再是明文 Key
        sysUserMapper.updateAiKey(userId,
                key == null ? null : cryptoService.encrypt(key));
        userCache.evict(userId);

        return Result.success(
                key == null ? "已清除个人 API Key" : "已保存个人 API Key",
                null);
    }

    /** 个人 AI 用量（用于个人中心展示今日额度） */
    @GetMapping("/me/ai-usage")
    public Result<Map<String, Object>> myAiUsage(
            HttpServletRequest request) {

        Long userId = currentUserId(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("systemKeyCallsToday",
                aiUsageService.countTodaySystemKey(userId));
        data.put("totalCallsToday", aiUsageService.countToday(userId));
        data.put("dailyLimit", aiRateLimiter.getDailyLimit());
        data.put("byKind", aiUsageService.countByKindToday(userId));
        return Result.success(data);
    }

    private SysUser currentUser(HttpServletRequest request) {
        SysUser user = sysUserMapper.findById(currentUserId(request));
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        return user;
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        if (value instanceof Long id) {
            return id;
        }
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
    }

    private UserVO toVO(SysUser user) {
        String masked = null;
        if (user.getAiApiKey() != null && !user.getAiApiKey().isBlank()) {
            masked = CryptoService.mask(
                    cryptoService.decrypt(user.getAiApiKey()));
        }
        return UserVO.of(user, masked);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 取真实客户端 IP（反代场景下优先 X-Forwarded-For） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
