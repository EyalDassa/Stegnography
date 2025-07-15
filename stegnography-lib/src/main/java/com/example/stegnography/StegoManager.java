package com.example.stegnography;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.stegnography.crypto.KeyManager;
import com.example.stegnography.stego.audio.LSBAudioSteganography;
import com.example.stegnography.stego.image.LSBImageSteganography;
import com.example.stegnography.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import android.graphics.BitmapFactory;

public class StegoManager {
    private final KeyManager keyManager = new KeyManager();
    private final LSBImageSteganography imageStego = new LSBImageSteganography();
    private final LSBAudioSteganography audioStego = new LSBAudioSteganography();

    public String generateKey() throws GeneralSecurityException {
        keyManager.generateKey();
        return keyManager.encodeKeyToBase64();
    }

    public void setKeyFromBase64(String keyB64) {
        keyManager.decodeKeyFromBase64(keyB64);
    }

    public boolean hasKey() {
        return keyManager.hasKey();
    }

    private SecretKey getSecretKey() {
        return keyManager.getSecretKey();
    }

    public Bitmap embedMessageInImage(Context context, Uri imageUri, String message) throws Exception {
        Bitmap src = ImageUtils.loadBitmapFromUri(context, imageUri);
        return imageStego.embed(src, message, keyManager.getSecretKey());
    }

    public String extractMessageFromImage(Context context, Uri imageUri) throws Exception {
        Bitmap stegoBitmap = ImageUtils.loadBitmapFromUri(context, imageUri);
        return imageStego.extract(stegoBitmap, keyManager.getSecretKey());
    }

    public File embedMessageInAudio(Context context, Uri audioUri, String message) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(audioUri);
        File inAudioFile = File.createTempFile("ina", ".wav", context.getCacheDir());
        FileOutputStream fos = new FileOutputStream(inAudioFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
        fos.close();
        is.close();
        File outAudioFile = File.createTempFile("out", ".wav", context.getCacheDir());
        audioStego.embed(inAudioFile, outAudioFile, message, keyManager.getSecretKey());
        return outAudioFile;
    }

    public String extractMessageFromAudio(Context context, Uri audioUri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(audioUri);
        File stegoAudioFile = File.createTempFile("stego", ".wav", context.getCacheDir());
        FileOutputStream fos = new FileOutputStream(stegoAudioFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
        fos.close();
        is.close();
        return audioStego.extract(stegoAudioFile, keyManager.getSecretKey());
    }

    public String extractRawStringFromImageFile(String imagePath) throws Exception {
        Bitmap stegoBmp = BitmapFactory.decodeFile(imagePath);
        return LSBImageSteganography.getRawExtractedString(stegoBmp);
    }

    public String extractRawStringFromAudioFile(String audioPath) throws Exception {
        File audioFile = new File(audioPath);
        return new LSBAudioSteganography().extractRawString(audioFile);
    }
} 