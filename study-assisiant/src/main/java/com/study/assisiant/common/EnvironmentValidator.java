package com.study.assisiant.common;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 生产环境启动自检。
 *
 * <p>密钥类配置缺失时，Spring 默认只会抛出
 * {@code PlaceholderResolutionException: Could not resolve placeholder 'XXX'}，
 * 对部署的人很不友好。这里改成在启动阶段就给出明确的中文清单，
 * 直接告诉运维缺了哪些变量、去哪里看文档。</p>
 */
@Component
public class EnvironmentValidator {

    private static final Logger log =
            LoggerFactory.getLogger(EnvironmentValidator.class);

    private static final String PROFILE_PROD = "prod";

    private final Environment environment;

    public EnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (!isProd()) {
            return;
        }

        List<String> missing = new ArrayList<>();
        if (isBlank(environment.getProperty("jwt.secret"))) {
            missing.add("JWT_SECRET");
        }
        if (isBlank(environment.getProperty("spring.datasource.password"))) {
            missing.add("DB_PASSWORD");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "生产环境缺少必需的环境变量：" + String.join("、", missing)
                            + "。请参考 DEPLOY.md 的「环境变量」章节补齐后重新部署。");
        }

        // 可选但强烈建议：缺失时只告警，不阻塞启动
        if (isBlank(environment.getProperty("app.crypto-key"))) {
            log.warn("未配置 CRYPTO_KEY，个人 AI Key 的加密密钥将由 JWT_SECRET 派生。"
                    + "生产环境建议单独设置 CRYPTO_KEY（更换它会导致已保存的 Key 需要重新填写）");
        }
        if (isBlank(environment.getProperty("ai.api-key"))) {
            log.warn("未配置 DEEPSEEK_API_KEY，用户必须在自己「个人中心」填写 API Key 才能使用 AI 功能");
        }

        String url = environment.getProperty("spring.datasource.url", "");
        if (url.contains("127.0.0.1") || url.contains("localhost")) {
            log.warn("当前数据库地址是本地回环地址（{}）。"
                    + "如果数据库在容器外，请确认 DB_URL 指向可访问的实例", url);
        }

        log.info("生产环境自检通过：profile={}，数据库={}",
                Arrays.toString(environment.getActiveProfiles()),
                maskJdbcUrl(url));
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles())
                .contains(PROFILE_PROD)
                || Arrays.asList(environment.getDefaultProfiles())
                .contains(PROFILE_PROD);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 日志里不带出数据库密码等敏感部分 */
    private String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "未配置";
        }
        int query = url.indexOf('?');
        return query > 0 ? url.substring(0, query) : url;
    }
}
