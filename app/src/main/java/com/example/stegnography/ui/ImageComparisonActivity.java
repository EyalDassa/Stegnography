package com.example.stegnography.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.stegnography.R;
import com.example.stegnography.stego.image.LSBImageSteganography;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

        // Set extracted message as before
        TextView tvExtracted = findViewById(R.id.tvExtracted);
        String stegoBmpPath = stegoPath;
        String extracted = "";
        if (stegoBmpPath != null) {
            try {
                android.graphics.Bitmap stegoBmp = android.graphics.BitmapFactory.decodeFile(stegoBmpPath);
                extracted = com.example.stegnography.stego.image.LSBImageSteganography.getRawExtractedString(stegoBmp);
            } catch (Exception e) {
                extracted = "Error extracting: " + e.getMessage();
            }
        }
        tvExtracted.setText("Extracted String from Stego Image:\n" + extracted);
    }
} 