package com.example.stegnography.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtils {
    public static Bitmap loadBitmapFromUri(Context context, Uri uri) throws Exception {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(is);
        }
    }

    public static void saveBitmapToUri(Context context, Bitmap bitmap, Uri uri) throws Exception {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
    }
} 