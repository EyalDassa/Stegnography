package com.example.stegnography;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.stegnography.StegoManager;
import java.io.File;
import android.net.Uri;

public class ImageComparisonActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_comparison);

        String originalPath = getIntent().getStringExtra("originalPath");
        String stegoPath = getIntent().getStringExtra("stegoPath");
        String diffPath = getIntent().getStringExtra("diffPath");

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        MediaDisplayFragment fragOriginal = new MediaDisplayFragment();
        Bundle argsOriginal = new Bundle();
        argsOriginal.putString(MediaDisplayFragment.ARG_LABEL, "Original Image");
        argsOriginal.putString(MediaDisplayFragment.ARG_PATH, originalPath);
        argsOriginal.putString(MediaDisplayFragment.ARG_TYPE, "image");
        argsOriginal.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelOriginal);
        fragOriginal.setArguments(argsOriginal);
        ft.replace(R.id.fragmentOriginal, fragOriginal);

        MediaDisplayFragment fragStego = new MediaDisplayFragment();
        Bundle argsStego = new Bundle();
        argsStego.putString(MediaDisplayFragment.ARG_LABEL, "Stego Image");
        argsStego.putString(MediaDisplayFragment.ARG_PATH, stegoPath);
        argsStego.putString(MediaDisplayFragment.ARG_TYPE, "image");
        argsStego.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelStego);
        fragStego.setArguments(argsStego);
        ft.replace(R.id.fragmentStego, fragStego);

        MediaDisplayFragment fragDiff = new MediaDisplayFragment();
        Bundle argsDiff = new Bundle();
        argsDiff.putString(MediaDisplayFragment.ARG_LABEL, "Difference Image (LSB)");
        argsDiff.putString(MediaDisplayFragment.ARG_PATH, diffPath);
        argsDiff.putString(MediaDisplayFragment.ARG_TYPE, "image");
        argsDiff.putInt(MediaDisplayFragment.ARG_STYLE, R.style.MediaLabelDiff);
        fragDiff.setArguments(argsDiff);
        ft.replace(R.id.fragmentDiff, fragDiff);

        ft.commit();

        String encrypted = getIntent().getStringExtra("encrypted");
        TextView tvEncrypted = findViewById(R.id.tvEncrypted);
        TextView tvExtracted = findViewById(R.id.tvExtracted);
        tvEncrypted.setText("Encrypted Message:\n" + (encrypted != null ? encrypted : ""));
        String keyB64 = getIntent().getStringExtra("key");
        StegoManager stegoManager = new StegoManager();
        stegoManager.setKeyFromBase64(keyB64);
        String decrypted = "";
        try {
            decrypted = stegoManager.extractMessageFromImage(this, Uri.fromFile(new File(stegoPath)));
        } catch (Exception e) {
            decrypted = "Error: " + e.getMessage();
        }
        tvExtracted.setText("Decrypted Message:\n" + decrypted);
    }
} 