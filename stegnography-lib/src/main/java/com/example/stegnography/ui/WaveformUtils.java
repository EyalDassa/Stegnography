package com.example.stegnography.ui;

import android.content.Context;
import android.view.ViewGroup;

public class WaveformUtils {
    public static WaveformView createWaveformView(Context context, byte[] pcm, ViewGroup.LayoutParams layoutParams) {
        WaveformView view = new WaveformView(context);
        if (layoutParams != null) {
            view.setLayoutParams(layoutParams);
        }
        view.setWaveform(pcm);
        return view;
    }
} 