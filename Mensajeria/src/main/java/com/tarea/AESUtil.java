package com.tarea;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.SecureRandom;

public class AESUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    // Clave hardcodeada para fines académicos (16 bytes para AES-128)
    private static final String SECRET_KEY = "1234567890123456";

    public static String[] encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return new String[]{
                Base64.getEncoder().encodeToString(cipherText), // Contenido cifrado
                Base64.getEncoder().encodeToString(iv)          // IV necesario para descifrar
        };
    }

    public static String decrypt(String cipherText, String ivStr) throws Exception {
        byte[] iv = Base64.getDecoder().decode(ivStr);
        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        byte[] plainText = cipher.doFinal(decodedCipherText);

        return new String(plainText, StandardCharsets.UTF_8);
    }
}