package com.study.assisiant;

import com.study.assisiant.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) {
        if (sysUserMapper.findByUsername("admin") != null) {
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");
        sysUserMapper.insert(admin);
    }
}
