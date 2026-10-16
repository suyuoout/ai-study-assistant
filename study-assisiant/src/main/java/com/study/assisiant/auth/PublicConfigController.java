package com.study.assisiant.auth;

import com.study.assisiant.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 无需登录即可读取的前端配置。
 *
 * <p>目前只暴露「是否开放注册」，让登录/注册页在关闭注册时
 * 直接显示提示，而不是让用户填完表单才收到 403。</p>
 */
@RestController
@RequestMapping("/api/public")
public class PublicConfigController {

    @Value("${app.allow-register:true}")
    private boolean allowRegister;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("allowRegister", allowRegister);
        return Result.success(data);
    }
}
