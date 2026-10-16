package com.study.assisiant.common;

import com.study.assisiant.mapper.SysUserMapper;
import com.study.assisiant.user.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 初始化管理员账号。
 *
 * <p>只有在显式开启 app.bootstrap-admin（环境变量 BOOTSTRAP_ADMIN=true）时
 * 才会创建，避免生产环境被写死一个 admin/admin123 弱口令账号。
 * 未提供初始密码时会生成随机密码并打印一次日志，并要求首次登录改密。</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SysUserMapper sysUserMapper;

    @Value("${app.bootstrap-admin:false}")
    private boolean bootstrapAdmin;

    @Value("${app.admin-username:admin}")
    private String adminUsername;

    @Value("${app.admin-password:}")
    private String adminPassword;

    public DataInitializer(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapAdmin) {
            return;
        }
        if (sysUserMapper.findByUsername(adminUsername) != null) {
            return;
        }

        boolean generated = adminPassword == null
                || adminPassword.isBlank();
        String rawPassword = generated
                ? randomPassword(16) : adminPassword;

        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname("管理员");
        admin.setEmail(null);
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        sysUserMapper.insert(admin);

        if (generated) {
            log.warn("已创建初始管理员 {}，随机初始密码：{}"
                            + "（首次登录后请立即修改密码）",
                    adminUsername, rawPassword);
        } else {
            log.warn("已创建初始管理员 {}，密码来自 ADMIN_PASSWORD 配置，"
                    + "首次登录后请立即修改密码", adminUsername);
        }
    }

    private String randomPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(
                    random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
