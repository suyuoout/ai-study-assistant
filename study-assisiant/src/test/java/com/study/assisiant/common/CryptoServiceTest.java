package com.study.assisiant.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServiceTest {

    private final CryptoService crypto =
            new CryptoService("unit-test-crypto-key", "unit-test-jwt-secret");

    @Test
    void encryptThenDecryptReturnsOriginal() {
        String plain = "sk-1234567890abcdefghijklmn";

        String encrypted = crypto.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertEquals(plain, crypto.decrypt(encrypted));
    }

    @Test
    void encryptionUsesRandomIv() {
        String plain = "sk-same-value-twice";
        assertNotEquals(crypto.encrypt(plain), crypto.encrypt(plain));
    }

    /** 历史明文数据必须仍然可用（兼容升级） */
    @Test
    void legacyPlainTextIsReturnedAsIs() {
        assertEquals("sk-legacy-plain-key", crypto.decrypt("sk-legacy-plain-key"));
    }

    @Test
    void nullAndBlankArePassedThrough() {
        assertNull(crypto.encrypt(null));
        assertNull(crypto.decrypt(null));
        assertEquals("", crypto.encrypt(" ").trim());
    }

    @Test
    void alreadyEncryptedValueIsNotDoubleEncrypted() {
        String encrypted = crypto.encrypt("sk-once");
        assertEquals(encrypted, crypto.encrypt(encrypted));
    }

    @Test
    void maskHidesMiddlePart() {
        assertEquals("****", CryptoService.mask("short"));
        assertNull(CryptoService.mask(null));
        assertEquals("sk-ab****cdef",
                CryptoService.mask("sk-abcdefghijklmncdef"));
    }

    @Test
    void isEncryptedDetectsPrefix() {
        assertTrue(crypto.isEncrypted(crypto.encrypt("sk-x")));
        assertFalse(crypto.isEncrypted("sk-plain"));
    }
}
