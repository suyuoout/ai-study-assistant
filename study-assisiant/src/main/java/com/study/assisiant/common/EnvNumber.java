package com.study.assisiant.common;

/**
 * 解析可能为空的数值配置。
 *
 * <p>平台上的环境变量很容易被填成空字符串（例如新建一条变量后没填值），
 * 而 Spring 的 {@code @Value} 遇到空字符串会直接抛转换异常、导致服务起不来。
 * 这里统一按「空值或非法值都回退默认值」处理。</p>
 */
public final class EnvNumber {

    private EnvNumber() {
    }

    public static int toInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static long toLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double toDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
