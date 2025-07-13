package com.example.stegnography.crypto;

import android.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class KeyManager {
    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey key) {
        this.secretKey = key;
    }

    public void generateKey() throws GeneralSecurityException {
        try {
            this.secretKey = CryptoUtils.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String encodeKeyToBase64() {
        if (secretKey == null) return "";
        return Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
    }

    public void decodeKeyFromBase64(String keyText) {
        byte[] keyBytes = Base64.decode(keyText, Base64.DEFAULT);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public boolean hasKey() {
        return secretKey != null;
    }
} 