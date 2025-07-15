package com.example.stegnography.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {
    private byte[] pcm;
    private Paint paint = new Paint();

    public WaveformView(Context context) { super(context); init(); }
    public WaveformView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(2f);
        paint.setAntiAlias(true);
    }

    public void setWaveform(byte[] pcm) {
        this.pcm = pcm;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pcm == null || pcm.length < 2) return;
        int width = getWidth();
        int height = getHeight();
        int samples = pcm.length / 2;
        int step = Math.max(1, samples / width);
        float centerY = height / 2f;
        for (int x = 0; x < width; x++) {
            int sampleIndex = x * step;
            if (sampleIndex * 2 + 1 >= pcm.length) break;
            int lo = pcm[sampleIndex * 2] & 0xFF;
            int hi = pcm[sampleIndex * 2 + 1] & 0xFF;
            short sample = (short) ((hi << 8) | lo);
            float norm = sample / 32768f;
            float y = centerY - norm * centerY;
            canvas.drawLine(x, centerY, x, y, paint);
        }
    }
} 