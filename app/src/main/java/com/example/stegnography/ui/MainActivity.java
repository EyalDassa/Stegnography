package com.example.stegnography.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stegnography.R;
import com.example.stegnography.crypto.CryptoUtils;
import com.example.stegnography.stego.image.LSBImageSteganography;
import com.example.stegnography.stego.audio.LSBAudioSteganography;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    private EditText etMessage, etKey;
    private Button btnGenerateKey, btnEncodeImage, btnDecodeImage, btnEncodeAudio, btnDecodeAudio;
    private ImageView ivOriginal, ivStego, ivDiff;
    private Bitmap lastOriginalBitmap, lastStegoBitmap;

    private SecretKey secretKey;
    private LSBImageSteganography imageStego;
    private LSBAudioSteganography audioStego;
    private Bitmap embeddedBitmap;
    private File inAudioFile, outAudioFile;

    private enum Operation { ENCODE_IMAGE, DECODE_IMAGE, ENCODE_AUDIO, DECODE_AUDIO }
    private Operation currentOp;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) handleImageUri(uri);
            });

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) handleAudioUri(uri);
            });

    private final ActivityResultLauncher<String> saveImageLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("image/png"),
            uri -> {
                if (uri != null) saveBitmap(uri);
            });

    private final ActivityResultLauncher<String> saveAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("audio/wav"),
            uri -> {
                if (uri != null) saveAudioFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etMessage = findViewById(R.id.etMessage);
        etKey = findViewById(R.id.etKey);
        btnGenerateKey = findViewById(R.id.btnGenerateKey);
        btnEncodeImage = findViewById(R.id.btnEncodeImage);
        btnDecodeImage = findViewById(R.id.btnDecodeImage);
        btnEncodeAudio = findViewById(R.id.btnEncodeAudio);
        btnDecodeAudio = findViewById(R.id.btnDecodeAudio);
        ivOriginal = findViewById(R.id.ivOriginal);
        ivStego = findViewById(R.id.ivStego);
        ivDiff = findViewById(R.id.ivDiff);

        imageStego = new LSBImageSteganography();
        audioStego = new LSBAudioSteganography();

        btnGenerateKey.setOnClickListener(v -> {
            try {
                secretKey = CryptoUtils.generateKey();
                String keyB64 = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
                etKey.setText(keyB64);
                Toast.makeText(this, "Key generated", Toast.LENGTH_SHORT).show();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "Key generation failed", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        btnEncodeImage.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.ENCODE_IMAGE;
            pickImageLauncher.launch("image/*");
        });

        btnDecodeImage.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.DECODE_IMAGE;
            pickImageLauncher.launch("image/*");
        });

        btnEncodeAudio.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.ENCODE_AUDIO;
            pickAudioLauncher.launch("audio/wav");
        });

        btnDecodeAudio.setOnClickListener(v -> {
            if (!ensureKey()) return;
            currentOp = Operation.DECODE_AUDIO;
            pickAudioLauncher.launch("audio/wav");
        });
    }

    private boolean ensureKey() {
        if (secretKey == null) {
            String keyText = etKey.getText().toString().trim();
            if (keyText.isEmpty()) {
                Toast.makeText(this, "Enter or generate a key first", Toast.LENGTH_SHORT).show();
                return false;
            }
            byte[] keyBytes = Base64.decode(keyText, Base64.DEFAULT);
            secretKey = new SecretKeySpec(keyBytes, "AES");
        }
        return true;
    }

    private void handleImageUri(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                Bitmap src = BitmapFactory.decodeStream(is);
                if (currentOp == Operation.ENCODE_IMAGE) {
                    String msg = etMessage.getText().toString();
                    embeddedBitmap = imageStego.embed(src, msg, secretKey);
                    lastOriginalBitmap = src;
                    lastStegoBitmap = embeddedBitmap;
                    runOnUiThread(() -> {
                        ivOriginal.setImageBitmap(lastOriginalBitmap);
                        ivStego.setImageBitmap(lastStegoBitmap);
                        ivDiff.setImageBitmap(null);
                        saveImageLauncher.launch("stego.png");
                    });
                } else {
                    // For decode, show all three images if available
                    final Bitmap stegoBitmap = src;
                    final Bitmap originalBitmap = lastOriginalBitmap;
                    final Bitmap diffBitmap = (originalBitmap != null)
                        ? LSBImageSteganography.createLSBDifferenceBitmap(originalBitmap, stegoBitmap)
                        : null;
                    runOnUiThread(() -> {
                        ivOriginal.setImageBitmap(originalBitmap);
                        ivStego.setImageBitmap(stegoBitmap);
                        ivDiff.setImageBitmap(diffBitmap);
                        String decoded;
                        try {
                            decoded = imageStego.extract(stegoBitmap, secretKey);
                        } catch (Exception e) {
                            decoded = "Error: " + e.getMessage();
                        }
                        Toast.makeText(this, "Decoded: " + decoded, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Image operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleAudioUri(Uri uri) {
        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                inAudioFile = File.createTempFile("in", ".wav", getCacheDir());
                try (FileOutputStream fos = new FileOutputStream(inAudioFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                }

                if (currentOp == Operation.ENCODE_AUDIO) {
                    String msg = etMessage.getText().toString();
                    outAudioFile = File.createTempFile("out", ".wav", getCacheDir());
                    audioStego.embed(inAudioFile, outAudioFile, msg, secretKey);
                    runOnUiThread(() -> saveAudioLauncher.launch("stego.wav"));
                } else {
                    String decoded = audioStego.extract(inAudioFile, secretKey);
                    runOnUiThread(() -> Toast.makeText(this, "Decoded: " + decoded, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Audio operation failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
        try (FileInputStream fis = new FileInputStream(outAudioFile);
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
