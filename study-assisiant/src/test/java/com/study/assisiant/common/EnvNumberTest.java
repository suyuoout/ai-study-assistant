package com.study.assisiant.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 环境变量为空是平台上的常见情况（新建变量但没填值），
 * 这里保证它回退默认值而不是让服务启动失败。
 */
class EnvNumberTest {

    @Test
    void blankValuesFallBackToDefaults() {
        assertEquals(8192, EnvNumber.toInt("", 8192));
        assertEquals(8192, EnvNumber.toInt(null, 8192));
        assertEquals(8192, EnvNumber.toInt("   ", 8192));
        assertEquals(50, EnvNumber.toInt("50", 8192));
        assertEquals(8192, EnvNumber.toInt("不是数字", 8192));

        assertEquals(60L, EnvNumber.toLong("", 60L));
        assertEquals(120L, EnvNumber.toLong("120", 60L));

        assertEquals(0.7, EnvNumber.toDouble("", 0.7));
        assertEquals(0.2, EnvNumber.toDouble("0.2", 0.7));
        assertEquals(0.7, EnvNumber.toDouble("abc", 0.7));
    }

    @Test
    void surroundingSpacesAreTolerated() {
        assertEquals(8192, EnvNumber.toInt(" 8192 ", 1));
        assertEquals(60L, EnvNumber.toLong(" 60 ", 1L));
    }
}
