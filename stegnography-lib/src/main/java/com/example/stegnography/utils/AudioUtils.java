package com.example.stegnography.utils;

import java.io.RandomAccessFile;

public class AudioUtils {
    public static byte[] loadPcmFromWav(String wavPath) {
        try (RandomAccessFile raf = new RandomAccessFile(wavPath, "r")) {
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
} 