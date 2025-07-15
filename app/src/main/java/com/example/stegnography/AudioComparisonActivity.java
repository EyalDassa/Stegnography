package com.example.stegnography;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.File;
import android.net.Uri;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.stegnography.StegoManager;

public class AudioComparisonActivity extends AppCompatActivity {
    private MediaPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_comparison);

        String originalPath = getIntent().getStringExtra("originalPath");
        String stegoPath = getIntent().getStringExtra("stegoPath");
        String diffPath = getIntent().getStringExtra("diffPath");
        String encrypted = getIntent().getStringExtra("encrypted");
        String extracted = getIntent().getStringExtra("extracted");

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        MediaDisplayFragment fragOriginal = new MediaDisplayFragment();
        Bundle argsOriginal = new Bundle();
        argsOriginal.putString(MediaDisplayFragment.ARG_LABEL, "Original Audio Waveform");
        argsOriginal.putString(MediaDisplayFragment.ARG_PATH, originalPath);
        argsOriginal.putString(MediaDisplayFragment.ARG_TYPE, "waveform");
        argsOriginal.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelOriginal);
        fragOriginal.setArguments(argsOriginal);
        ft.replace(R.id.fragmentOriginalAudio, fragOriginal);

        MediaDisplayFragment fragStego = new MediaDisplayFragment();
        Bundle argsStego = new Bundle();
        argsStego.putString(MediaDisplayFragment.ARG_LABEL, "Stego Audio Waveform");
        argsStego.putString(MediaDisplayFragment.ARG_PATH, stegoPath);
        argsStego.putString(MediaDisplayFragment.ARG_TYPE, "waveform");
        argsStego.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelStego);
        fragStego.setArguments(argsStego);
        ft.replace(R.id.fragmentStegoAudio, fragStego);

        MediaDisplayFragment fragDiff = new MediaDisplayFragment();
        Bundle argsDiff = new Bundle();
        argsDiff.putString(MediaDisplayFragment.ARG_LABEL, "Difference Audio Waveform");
        argsDiff.putString(MediaDisplayFragment.ARG_PATH, diffPath);
        argsDiff.putString(MediaDisplayFragment.ARG_TYPE, "waveform");
        argsDiff.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelDiff);
        fragDiff.setArguments(argsDiff);
        ft.replace(R.id.fragmentDiffAudio, fragDiff);

        ft.commit();

        Button btnPlayOriginal = findViewById(R.id.btnPlayOriginal);
        Button btnPlayStego = findViewById(R.id.btnPlayStego);
        Button btnPlayDiff = findViewById(R.id.btnPlayDiff);
        TextView tvEncrypted = findViewById(R.id.tvEncryptedAudio);
        TextView tvExtracted = findViewById(R.id.tvExtractedAudio);

        btnPlayOriginal.setOnClickListener(v -> playAudio(originalPath));
        btnPlayStego.setOnClickListener(v -> playAudio(stegoPath));
        btnPlayDiff.setOnClickListener(v -> playAudio(diffPath));
        tvEncrypted.setText("Encrypted Message:\n" + (encrypted != null ? encrypted : ""));
        String decrypted = "";
        String keyB64 = getIntent().getStringExtra("key");
        StegoManager stegoManager = new StegoManager();
        stegoManager.setKeyFromBase64(keyB64);
        try {
            decrypted = stegoManager.extractMessageFromAudio(this, Uri.fromFile(new File(stegoPath)));
        } catch (Exception e) {
            decrypted = "Error: " + e.getMessage();
        }
        tvExtracted.setText("Decrypted Message:\n" + decrypted);
    }

    private void playAudio(String path) {
        if (player != null) {
            player.release();
        }
        player = new MediaPlayer();
        try {
            player.setDataSource(path);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
        }
        super.onDestroy();
    }
} 