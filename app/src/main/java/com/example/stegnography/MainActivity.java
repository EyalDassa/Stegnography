package com.example.stegnography;

import android.content.Intent;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;

import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stegnography.stego.audio.LSBAudioSteganography;
import com.example.stegnography.stego.image.LSBImageSteganography;
import com.example.stegnography.utils.ImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    private EditText etMessage, etKey;
    private Button btnGenerateKey, btnEncodeImage, btnDecodeImage;
    private Button btnEncodeAudio, btnDecodeAudio;

    private Bitmap lastOriginalBitmap, lastStegoBitmap;

    private StegoManager stegoManager;
    private Bitmap embeddedBitmap;
    private File lastOriginalAudioFile, lastStegoAudioFile;

    private enum Operation { ENCODE_IMAGE, DECODE_IMAGE, ENCODE_AUDIO, DECODE_AUDIO }
    private Operation currentOp;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) handleImageUri(uri);
            });

    private final ActivityResultLauncher<String> saveImageLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("image/png"),
            uri -> {
                if (uri != null) saveBitmap(uri);
            });

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) handleAudioUri(uri);
        });
    private final ActivityResultLauncher<String> saveAudioLauncher = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("audio/wav"),
        uri -> {
            if (uri != null) saveAudioFile(uri);
        });

    private Uri pendingOriginalAudioUri;
    private final ActivityResultLauncher<String> pickStegoAudioLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null && pendingOriginalAudioUri != null) {
                handleAudioDecodeUris(pendingOriginalAudioUri, uri);
                pendingOriginalAudioUri = null;
            }
        });
    private final ActivityResultLauncher<String> pickOriginalAudioLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                pendingOriginalAudioUri = uri;
                pickStegoAudioLauncher.launch("audio/*");
            }
        });

    private Uri pendingOriginalImageUri;
    private final ActivityResultLauncher<String> pickStegoImageLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null && pendingOriginalImageUri != null) {
                handleImageDecodeUris(pendingOriginalImageUri, uri);
                pendingOriginalImageUri = null;
            }
        });
    private final ActivityResultLauncher<String> pickOriginalImageLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                pendingOriginalImageUri = uri;
                pickStegoImageLauncher.launch("image/*");
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();
        setupListeners();
        stegoManager = new StegoManager();
    }

    private void setupUI() {
        etMessage = findViewById(R.id.etMessage);
        etKey = findViewById(R.id.etKey);
        btnGenerateKey = findViewById(R.id.btnGenerateKey);
        btnEncodeImage = findViewById(R.id.btnEncodeImage);
        btnDecodeImage = findViewById(R.id.btnDecodeImage);
        btnEncodeAudio = findViewById(R.id.btnEncodeAudio);
        btnDecodeAudio = findViewById(R.id.btnDecodeAudio);
    }

    private void setupListeners() {
        btnGenerateKey.setOnClickListener(v -> generateKey());
        btnEncodeImage.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.ENCODE_IMAGE;
            pickImageLauncher.launch("image/*");
        });
        btnDecodeImage.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.DECODE_IMAGE;
            pickOriginalImageLauncher.launch("image/*");
        });
        btnEncodeAudio.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.ENCODE_AUDIO;
            pickAudioLauncher.launch("audio/*");
        });
        btnDecodeAudio.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.DECODE_AUDIO;
            pickOriginalAudioLauncher.launch("audio/*");
        });
    }

    private void generateKey() {
        try {
            String keyB64 = stegoManager.generateKey();
            etKey.setText(keyB64);
            Toast.makeText(this, "Key generated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Key generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean ensureKey() {
        if (!stegoManager.hasKey()) {
            String keyText = etKey.getText().toString().trim();
            if (keyText.isEmpty()) {
                Toast.makeText(this, "Enter or generate a key first", Toast.LENGTH_SHORT).show();
                return false;
            }
            stegoManager.setKeyFromBase64(keyText);
        }
        return true;
    }

    private void handleImageUri(Uri uri) {
        new Thread(() -> {
            try {
                if (currentOp == Operation.ENCODE_IMAGE) {
                    String msg = etMessage.getText().toString();
                    embeddedBitmap = stegoManager.embedMessageInImage(this, uri, msg);
                    lastOriginalBitmap = ImageUtils.loadBitmapFromUri(this, uri);
                    lastStegoBitmap = embeddedBitmap;
                    runOnUiThread(() -> saveImageLauncher.launch("stego.png"));
                } else {
                    final Bitmap stegoBitmap = ImageUtils.loadBitmapFromUri(this, uri);
                    final Bitmap originalBitmap = lastOriginalBitmap;
                    final Bitmap diffBitmap = (originalBitmap != null)
                        ? LSBImageSteganography.createLSBDifferenceBitmap(originalBitmap, stegoBitmap)
                        : null;
                    // Save bitmaps to temp files
                    File origFile = File.createTempFile("original", ".png", getCacheDir());
                    File stegoFile = File.createTempFile("stego", ".png", getCacheDir());
                    File diffFile = File.createTempFile("diff", ".png", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(origFile)) {
                        originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                    try (FileOutputStream fos = new FileOutputStream(stegoFile)) {
                        stegoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                    if (diffBitmap != null) {
                        try (FileOutputStream fos = new FileOutputStream(diffFile)) {
                            diffBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        }
                    }
                    String encrypted = stegoManager.extractRawStringFromImageFile(stegoFile.getAbsolutePath());
                    Intent intent = new Intent(this, ImageComparisonActivity.class);
                    intent.putExtra("originalPath", origFile.getAbsolutePath());
                    intent.putExtra("stegoPath", stegoFile.getAbsolutePath());
                    intent.putExtra("diffPath", diffBitmap != null ? diffFile.getAbsolutePath() : null);
                    intent.putExtra("encrypted", encrypted);
                    intent.putExtra("key", etKey.getText().toString().trim());
                    runOnUiThread(() -> startActivity(intent));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Image operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleAudioUri(Uri uri) {
        new Thread(() -> {
            try {
                if (currentOp == Operation.ENCODE_AUDIO) {
                    String msg = etMessage.getText().toString();
                    File outAudioFile = stegoManager.embedMessageInAudio(this, uri, msg);
                    lastStegoAudioFile = outAudioFile;
                    runOnUiThread(() -> saveAudioLauncher.launch("stego.wav"));
                } else {
                    File stegoAudioFile = File.createTempFile("stego", ".wav", getCacheDir());
                    try (InputStream is = getContentResolver().openInputStream(uri);
                         FileOutputStream fos = new FileOutputStream(stegoAudioFile)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                    File originalAudioFile = lastOriginalAudioFile;
                    File diffAudioFile = null;
                    if (originalAudioFile != null) {
                        diffAudioFile = File.createTempFile("diff", ".wav", getCacheDir());
                        LSBAudioSteganography.createLSBDifferenceWav(originalAudioFile, stegoAudioFile, diffAudioFile);
                    }
                    String encrypted = stegoManager.extractRawStringFromAudioFile(stegoAudioFile.getAbsolutePath());
                    Intent intent = new Intent(this, AudioComparisonActivity.class);
                    intent.putExtra("originalPath", originalAudioFile != null ? originalAudioFile.getAbsolutePath() : null);
                    intent.putExtra("stegoPath", stegoAudioFile.getAbsolutePath());
                    intent.putExtra("diffPath", diffAudioFile != null ? diffAudioFile.getAbsolutePath() : null);
                    intent.putExtra("encrypted", encrypted);
                    intent.putExtra("key", etKey.getText().toString().trim());
                    runOnUiThread(() -> startActivity(intent));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Audio operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleAudioDecodeUris(Uri originalUri, Uri stegoUri) {
        new Thread(() -> {
            try {
                // Copy original audio to temp file
                File originalAudioFile = File.createTempFile("original", ".wav", getCacheDir());
                try (InputStream is = getContentResolver().openInputStream(originalUri);
                     FileOutputStream fos = new FileOutputStream(originalAudioFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                }
                // Copy stego audio to temp file
                File stegoAudioFile = File.createTempFile("stego", ".wav", getCacheDir());
                try (InputStream is = getContentResolver().openInputStream(stegoUri);
                     FileOutputStream fos = new FileOutputStream(stegoAudioFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                }
                // Create diff
                File diffAudioFile = File.createTempFile("diff", ".wav", getCacheDir());
                LSBAudioSteganography.createLSBDifferenceWav(originalAudioFile, stegoAudioFile, diffAudioFile);
                // Extract message
                String encrypted = stegoManager.extractRawStringFromAudioFile(stegoAudioFile.getAbsolutePath());
                Intent intent = new Intent(this, AudioComparisonActivity.class);
                intent.putExtra("originalPath", originalAudioFile.getAbsolutePath());
                intent.putExtra("stegoPath", stegoAudioFile.getAbsolutePath());
                intent.putExtra("diffPath", diffAudioFile.getAbsolutePath());
                intent.putExtra("encrypted", encrypted);
                intent.putExtra("key", etKey.getText().toString().trim());
                runOnUiThread(() -> startActivity(intent));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Audio operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleImageDecodeUris(Uri originalUri, Uri stegoUri) {
        new Thread(() -> {
            try {
                // Copy original image to temp file
                Bitmap originalBitmap = ImageUtils.loadBitmapFromUri(this, originalUri);
                File originalFile = File.createTempFile("original", ".png", getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(originalFile)) {
                    originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                // Copy stego image to temp file
                Bitmap stegoBitmap = ImageUtils.loadBitmapFromUri(this, stegoUri);
                File stegoFile = File.createTempFile("stego", ".png", getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(stegoFile)) {
                    stegoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                // Create diff
                File diffFile = File.createTempFile("diff", ".png", getCacheDir());
                Bitmap diffBitmap = LSBImageSteganography.createLSBDifferenceBitmap(originalBitmap, stegoBitmap);
                try (FileOutputStream fos = new FileOutputStream(diffFile)) {
                    diffBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                // Extract encrypted message
                String encrypted = stegoManager.extractRawStringFromImageFile(stegoFile.getAbsolutePath());
                Intent intent = new Intent(this, ImageComparisonActivity.class);
                intent.putExtra("originalPath", originalFile.getAbsolutePath());
                intent.putExtra("stegoPath", stegoFile.getAbsolutePath());
                intent.putExtra("diffPath", diffFile.getAbsolutePath());
                intent.putExtra("encrypted", encrypted);
                intent.putExtra("key", etKey.getText().toString().trim());
                runOnUiThread(() -> startActivity(intent));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Image operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveBitmap(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            embeddedBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save image failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAudioFile(Uri uri) {
        try (FileInputStream fis = new FileInputStream(lastStegoAudioFile);
             OutputStream os = getContentResolver().openOutputStream(uri)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) > 0) os.write(buf, 0, len);
            Toast.makeText(this, "Audio saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save audio failed", Toast.LENGTH_SHORT).show();
        }
    }
}
