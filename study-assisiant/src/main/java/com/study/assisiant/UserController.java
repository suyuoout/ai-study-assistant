
package com.study.assisiant;

import com.study.assisiant.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @org.springframework.beans.factory.annotation.Value(
            "${app.allow-register:true}")
    private boolean allowRegister;

    public UserController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping
    public List<UserVO> list() {
        List<SysUser> users = sysUserMapper.findAll();

        return users.stream()
                .map(this::toVO)
                .toList();
    }


    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterRequest request) {

        if (!allowRegister) {
            return Result.error(403, "当前环境未开放注册");
        }

        // 1. 校验用户名
        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            return Result.error(400, "用户名不能为空");
        }

        String username = request.getUsername().trim();

        // 2. 校验密码
        if (request.getPassword() == null
                || request.getPassword().length() < 8) {
            return Result.error(400, "密码至少需要8位");
        }

        // 3. 检查用户名是否已存在
        if (sysUserMapper.findByUsername(username) != null) {
            return Result.error(409, "用户名已存在");
        }

        // 4. 创建用户对象
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole("USER");

        // 5. 保存用户
        int rows = sysUserMapper.insert(user);

        if (rows != 1) {
            return Result.error(500, "注册失败");
        }

        // 6. 返回不包含密码的用户信息
        UserVO userVO = toVO(user);

        return Result.success("注册成功", userVO);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {

        // 1. 检查用户名
        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            return Result.error(400, "用户名不能为空");
        }

        // 2. 检查密码
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            return Result.error(400, "密码不能为空");
        }

        // 3. 根据用户名查询用户
        SysUser user = sysUserMapper.findByUsername(
                request.getUsername().trim());

        // 4. 验证用户名和密码
        if (user == null
                || !passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        // 5. 生成不包含密码的用户信息
        UserVO userVO = toVO(user);


// 生成JWT
        String token = JwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );


// 返回用户信息 + token
        LoginResponse response =
                new LoginResponse(userVO, token);


        return Result.success("登录成功", response);


    }

    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(toVO(user));
    }

    @PutMapping("/me")
    public Result<UserVO> updateMe(
            @RequestBody UpdateProfileRequest req,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        sysUserMapper.updateProfile(
                userId, req.getNickname(), req.getEmail());

        SysUser user = sysUserMapper.findById(userId);
        return Result.success("修改成功", toVO(user));
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getCreateTime()
        );
        vo.setRole(user.getRole());
        return vo;
    }
}
