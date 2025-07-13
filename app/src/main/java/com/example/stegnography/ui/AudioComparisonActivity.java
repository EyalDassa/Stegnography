package com.example.stegnography.ui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.stegnography.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import com.example.stegnography.ui.WaveformView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AudioComparisonActivity extends AppCompatActivity {
    private MediaPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_comparison);

        String originalPath = getIntent().getStringExtra("originalPath");
        String stegoPath = getIntent().getStringExtra("stegoPath");
        String diffPath = getIntent().getStringExtra("diffPath");
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
        TextView tvExtracted = findViewById(R.id.tvExtractedAudio);

        btnPlayOriginal.setOnClickListener(v -> playAudio(originalPath));
        btnPlayStego.setOnClickListener(v -> playAudio(stegoPath));
        btnPlayDiff.setOnClickListener(v -> playAudio(diffPath));
        tvExtracted.setText("Extracted Message:\n" + (extracted != null ? extracted : ""));
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

    // Static method for fragment to use
    public static byte[] loadPcmFromWavStatic(String wavPath) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(wavPath, "r")) {
            raf.seek(12);
            while (true) {
                byte[] chunkId = new byte[4];
                raf.readFully(chunkId);
                int chunkSize = Integer.reverseBytes(raf.readInt());
                String id = new String(chunkId, "US-ASCII");
                if (id.equals("data")) {
                    byte[] pcm = new byte[chunkSize];
                    raf.readFully(pcm);
                    return pcm;
                } else {
                    raf.skipBytes(chunkSize);
                }
            }
        } catch (Exception e) {
            return null;
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