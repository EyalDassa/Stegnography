package com.example.stegnography.crypto;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class CryptoUtils {
    public static SecretKey generateKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        return kg.generateKey();
    }

    public static String encrypt(String plain, SecretKey key) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(ct, Base64.DEFAULT);
    }

    public static String decrypt(String ctBase64, SecretKey key) throws Exception {
        byte[] ct = Base64.decode(ctBase64, Base64.DEFAULT);
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] pt = c.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }
}
