package com.example.stegnography;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stegnography.ui.WaveformView;
import com.example.stegnography.ui.WaveformUtils;

public class MediaDisplayFragment extends Fragment {
    public static final String ARG_LABEL = "label";
    public static final String ARG_PATH = "path";
    public static final String ARG_TYPE = "type"; // "image" or "waveform"
    public static final String ARG_STYLE = "style"; // style resource id (optional)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String label = args != null ? args.getString(ARG_LABEL, "") : "";
        String path = args != null ? args.getString(ARG_PATH, null) : null;
        String type = args != null ? args.getString(ARG_TYPE, "image") : "image";
        int styleRes = args != null ? args.getInt(ARG_STYLE, 0) : 0;

        View root = inflater.inflate(R.layout.fragment_media_display, container, false);
        TextView tvLabel = root.findViewById(R.id.tvMediaLabel);
        tvLabel.setText(label);
        if (styleRes != 0) {
            tvLabel.setTextAppearance(requireContext(), styleRes);
        }
        ViewGroup mediaContainer = root.findViewById(R.id.mediaContainer);
        mediaContainer.removeAllViews();
        if (!TextUtils.isEmpty(path)) {
            if ("image".equals(type)) {
                android.widget.ImageView imageView = new android.widget.ImageView(requireContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                Bitmap bmp = BitmapFactory.decodeFile(path);
                imageView.setImageBitmap(bmp);
                imageView.setBackgroundResource(R.drawable.media_bg_image);
                mediaContainer.addView(imageView);
            } else if ("waveform".equals(type)) {
                android.view.ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                WaveformView waveformView = WaveformUtils.createWaveformView(requireContext(), com.example.stegnography.utils.AudioUtils.loadPcmFromWav(path), params);
                waveformView.setBackgroundResource(R.drawable.media_bg_waveform);
                waveformView.setPadding(4, 4, 4, 4);
                waveformView.setClipToOutline(true);
                mediaContainer.addView(waveformView);
            }
        }
        return root;
    }
} 