package com.opsflow.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 加解密工具（GCM 模式，用于敏感凭据与脚本内容加密存储）。
 * <p>
 * 密钥通过环境变量 {@code OPSFLOW_AES_KEY} 注入，经 SHA-256 派生为 32 字节。
 * 每个密文前都会附带一个 12 字节的随机 IV，格式为：Base64(iv + tag + cipherText)。
 * </p>
 * <p>
 * 为兼容历史 ECB 密文，可选择性设置环境变量 {@code OPSFLOW_AES_LEGACY_KEY}，
 * 解密失败时会回退到旧的 ECB 模式再尝试一次；新写入的数据全部使用 GCM 模式。
 * 若未设置 LEGACY_KEY，则仅支持 GCM 模式。
 * </p>
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION_GCM = "AES/GCM/NoPadding";
    private static final String TRANSFORMATION_ECB = "AES/ECB/PKCS5Padding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /** 从环境变量读取密钥并派生 32 字节 AES 密钥 */
    private static final byte[] KEY = deriveKey();
    /** 旧 ECB 模式的兼容密钥，通过环境变量 OPSFLOW_AES_LEGACY_KEY 注入，未设置则为 null */
    private static final byte[] LEGACY_KEY = deriveLegacyKey();

    private AesUtil() {
    }

    private static byte[] deriveKey() {
        String envKey = System.getenv("OPSFLOW_AES_KEY");
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException("环境变量 OPSFLOW_AES_KEY 未设置，无法初始化 AES 密钥。" +
                    "请在启动应用前设置强度足够的密钥，例如：export OPSFLOW_AES_KEY=$(openssl rand -hex 32)");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(envKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("初始化 AES 密钥失败", e);
        }
    }

    private static byte[] deriveLegacyKey() {
        String envKey = System.getenv("OPSFLOW_AES_LEGACY_KEY");
        if (envKey == null || envKey.isBlank()) {
            return null;
        }
        return envKey.getBytes(StandardCharsets.UTF_8);
    }

    /** GCM 加密为 Base64 字符串 */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, GCM_IV_LENGTH, cipherBytes.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /**
     * 解密 Base64 密文。
     * 先尝试 GCM 模式；若配置了 LEGACY_KEY 且 GCM 失败时回退到旧 ECB 模式，兼容历史数据。
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            return decryptGcm(cipherText);
        } catch (Exception e) {
            if (LEGACY_KEY != null) {
                try {
                    return decryptEcb(cipherText);
                } catch (Exception legacyEx) {
                    throw new IllegalStateException("AES 解密失败（GCM + ECB 回退均失败）", legacyEx);
                }
            }
            throw new IllegalStateException("AES 解密失败（GCM 模式）", e);
        }
    }

    private static String decryptGcm(String cipherText) throws Exception {
        byte[] combined = Base64.getDecoder().decode(cipherText);
        if (combined.length <= GCM_IV_LENGTH) {
            throw new IllegalArgumentException("密文长度不足");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    private static String decryptEcb(String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(LEGACY_KEY, ALGORITHM));
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
    }
}
