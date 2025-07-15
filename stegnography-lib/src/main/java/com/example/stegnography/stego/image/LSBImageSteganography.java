package com.example.stegnography.stego.image;

import android.graphics.Bitmap;
import com.example.stegnography.crypto.CryptoUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

public class LSBImageSteganography {
    private static final String DELIM = "<<<END>>>";

    public Bitmap embed(Bitmap src, String message, SecretKey key) throws Exception {
        String encrypted = CryptoUtils.encrypt(message, key) + DELIM;
        byte[] data = encrypted.getBytes(StandardCharsets.UTF_8);
        int totalBits = data.length * 8;

        Bitmap dst = src.copy(Bitmap.Config.ARGB_8888, true);
        int width = dst.getWidth(), height = dst.getHeight();
        int bitIndex = 0;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= totalBits) break outer;

                int pixel = dst.getPixel(x, y);
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel) & 0xFF;

                // Get the bit to embed
                int byteIndex = bitIndex / 8;
                int bitInByte = 7 - (bitIndex % 8); // MSB first
                int bit = (data[byteIndex] >> bitInByte) & 1;
                b = (b & 0xFE) | bit;

                int newPixel = (a << 24) | (r << 16) | (g << 8) | b;
                dst.setPixel(x, y, newPixel);
                bitIndex++;
            }
        }
        return dst;
    }

    public String extract(Bitmap src, SecretKey key) throws Exception {
        int width = src.getWidth(), height = src.getHeight();
        // We'll collect bits and reconstruct bytes manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitIndex = 0;
        int currentByte = 0;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int b = src.getPixel(x, y) & 0xFF;
                int bit = b & 1;
                currentByte = (currentByte << 1) | bit;
                bitIndex++;
                if (bitIndex % 8 == 0) {
                    baos.write(currentByte);
                    String s = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    if (s.contains(DELIM)) break outer;
                    currentByte = 0;
                }
            }
        }
        byte[] allBytes = baos.toByteArray();
        String collected = new String(allBytes, StandardCharsets.UTF_8);
        int delimIndex = collected.indexOf(DELIM);
        if (delimIndex == -1) {
            throw new Exception("No hidden message found or wrong key/image.");
        }
        String encMsg = collected.substring(0, delimIndex);
        return CryptoUtils.decrypt(encMsg, key);
    }

    // Debug function: show the bitwise difference between two images as a string
    public static String extractDifferenceAsString(Bitmap original, Bitmap stego, int messageLength) {
        int width = original.getWidth(), height = original.getHeight();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitIndex = 0;
        int currentByte = 0;
        int totalBits = (messageLength + DELIM.length()) * 8;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= totalBits) break outer;
                int origB = original.getPixel(x, y) & 0xFF;
                int stegoB = stego.getPixel(x, y) & 0xFF;
                int bit = stegoB & 1;
                currentByte = (currentByte << 1) | bit;
                bitIndex++;
                if (bitIndex % 8 == 0) {
                    baos.write(currentByte);
                    currentByte = 0;
                }
            }
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    // Visualize LSB differences between two images
    public static Bitmap createLSBDifferenceBitmap(Bitmap original, Bitmap stego) {
        int width = Math.min(original.getWidth(), stego.getWidth());
        int height = Math.min(original.getHeight(), stego.getHeight());
        Bitmap diff = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int origB = original.getPixel(x, y) & 0xFF;
                int stegoB = stego.getPixel(x, y) & 0xFF;
                int lsbOrig = origB & 1;
                int lsbStego = stegoB & 1;
                int color = (lsbOrig == lsbStego) ? 0xFF000000 : 0xFFFFFFFF; // black if same, white if different
                diff.setPixel(x, y, color);
            }
        }
        return diff;
    }

    // Extract the raw embedded string from the image (without decryption)
    public static String getRawExtractedString(Bitmap src) {
        int width = src.getWidth(), height = src.getHeight();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitIndex = 0;
        int currentByte = 0;
        StringBuilder collected = new StringBuilder();
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int b = src.getPixel(x, y) & 0xFF;
                int bit = b & 1;
                currentByte = (currentByte << 1) | bit;
                bitIndex++;
                if (bitIndex % 8 == 0) {
                    baos.write(currentByte);
                    String s = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    if (s.contains(DELIM)) break outer;
                    currentByte = 0;
                }
            }
        }
        byte[] allBytes = baos.toByteArray();
        String result = new String(allBytes, StandardCharsets.UTF_8);
        int delimIndex = result.indexOf(DELIM);
        if (delimIndex != -1) {
            return result.substring(0, delimIndex);
        } else {
            return result;
        }
    }
}