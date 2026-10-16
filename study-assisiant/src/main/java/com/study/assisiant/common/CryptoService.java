package com.study.assisiant.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感字段加解密（AES-256/GCM）。
 *
 * <p>密文格式：{@code enc:v1:base64(iv || ciphertext)}。
 * 老数据里没有前缀的明文值会被原样读出（兼容历史数据），
 * 并在下次保存时自动升级为密文。</p>
 */
@Component
public class CryptoService {

    private static final Logger log =
            LoggerFactory.getLogger(CryptoService.class);

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(
            @Value("${app.crypto-key:}") String cryptoKey,
            @Value("${jwt.secret:}") String jwtSecret) {

        String material = (cryptoKey != null && !cryptoKey.isBlank())
                ? cryptoKey : jwtSecret;
        if (material == null || material.isBlank()) {
            // 不在这里抛异常：让 EnvironmentValidator 汇总给出可读的缺失清单，
            // 这里只在真的要用到加解密的时候再报错。
            log.error("未配置加密密钥（CRYPTO_KEY / JWT_SECRET），"
                    + "个人 AI Key 的加解密功能将不可用");
            this.key = null;
            return;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化加密组件失败", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException(
                    "加密密钥未配置：请设置环境变量 JWT_SECRET 或 CRYPTO_KEY");
        }
    }

    /** 加密；空值或已加密的值原样返回 */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()
                || isEncrypted(plainText)) {
            return plainText;
        }
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length,
                    encrypted.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /** 解密；非密文（历史明文数据）原样返回 */
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        if (!isEncrypted(stored)) {
            return stored;
        }
        requireKey();
        try {
            byte[] combined = Base64.getDecoder()
                    .decode(stored.substring(PREFIX.length()));
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度异常");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0,
                    cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 密钥轮换等情况下旧密文无法解开，降级为「无个人 Key」
            log.warn("解密敏感字段失败，将按未配置处理", e);
            return null;
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** 只保留首尾少量字符用于展示，例如 sk-ab****wxyz */
    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 5) + "****"
                + secret.substring(secret.length() - 4);
    }
}
