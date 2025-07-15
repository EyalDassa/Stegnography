package com.example.stegnography.stego.audio;

import com.example.stegnography.crypto.CryptoUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

public class LSBAudioSteganography {
    private static final String DELIM = "<<<END>>>";

    // Helper to find the start and length of the 'data' chunk in a WAV file
    private static int[] findWavDataChunk(RandomAccessFile raf) throws Exception {
        raf.seek(12); // skip RIFF header
        while (true) {
            byte[] chunkId = new byte[4];
            raf.readFully(chunkId);
            int chunkSize = Integer.reverseBytes(raf.readInt());
            String id = new String(chunkId, "US-ASCII");
            if (id.equals("data")) {
                int dataStart = (int) raf.getFilePointer();
                return new int[]{dataStart, chunkSize};
            } else {
                raf.skipBytes(chunkSize);
            }
        }
    }

    public void embed(File inWav, File outWav, String message, SecretKey key) throws Exception {
        // Copy input WAV to output, then modify the data chunk in-place
        try (FileInputStream fis = new FileInputStream(inWav);
             FileOutputStream fos = new FileOutputStream(outWav)) {
            byte[] allBytes = fis.readAllBytes();
            fos.write(allBytes);
        }
        try (RandomAccessFile raf = new RandomAccessFile(outWav, "rw")) {
            int[] dataInfo = findWavDataChunk(raf);
            int dataStart = dataInfo[0];
            int dataLen = dataInfo[1];
            raf.seek(dataStart);
            byte[] pcm = new byte[dataLen];
            raf.readFully(pcm);

            // encrypt + delimiter
            String enc = CryptoUtils.encrypt(message, key) + DELIM;
            byte[] data = enc.getBytes("UTF-8");
            int totalBits = data.length * 8;

            int bitIndex = 0;
            for (int i = 0; i < dataLen / 2 && bitIndex < totalBits; i++, bitIndex++) {
                int byteIndex = bitIndex / 8;
                int bitInByte = 7 - (bitIndex % 8);
                int bit = (data[byteIndex] >> bitInByte) & 1;
                int lo = pcm[i * 2] & 0xFF;
                int hi = pcm[i * 2 + 1] & 0xFF;
                short sample = (short) ((hi << 8) | lo);
                sample = (short) ((sample & ~1) | bit);
                pcm[i * 2] = (byte) (sample & 0xFF);
                pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }
            raf.seek(dataStart);
            raf.write(pcm);
        }
    }

    public String extract(File inWav, SecretKey key) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(inWav, "r")) {
            int[] dataInfo = findWavDataChunk(raf);
            int dataStart = dataInfo[0];
            int dataLen = dataInfo[1];
            raf.seek(dataStart);
            byte[] pcm = new byte[dataLen];
            raf.readFully(pcm);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bitIndex = 0;
            int currentByte = 0;
            for (int i = 0; i < dataLen / 2; i++) {
                int lo = pcm[i * 2] & 0xFF;
                int hi = pcm[i * 2 + 1] & 0xFF;
                short sample = (short) ((hi << 8) | lo);
                int bit = sample & 1;
                currentByte = (currentByte << 1) | bit;
                bitIndex++;
                if (bitIndex % 8 == 0) {
                    baos.write(currentByte);
                    String s = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    if (s.contains(DELIM)) break;
                    currentByte = 0;
                }
            }
            byte[] all = baos.toByteArray();
            String coll = new String(all, StandardCharsets.UTF_8);
            int delimIndex = coll.indexOf(DELIM);
            if (delimIndex == -1) {
                throw new Exception("No hidden message found or wrong key/audio file.");
            }
            String encMsg = coll.substring(0, delimIndex);
            return CryptoUtils.decrypt(encMsg, key);
        }
    }

    public String extractRawString(File inWav) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(inWav, "r")) {
            int[] dataInfo = findWavDataChunk(raf);
            int dataStart = dataInfo[0];
            int dataLen = dataInfo[1];
            raf.seek(dataStart);
            byte[] pcm = new byte[dataLen];
            raf.readFully(pcm);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bitIndex = 0;
            int currentByte = 0;
            for (int i = 0; i < dataLen / 2; i++) {
                int lo = pcm[i * 2] & 0xFF;
                int hi = pcm[i * 2 + 1] & 0xFF;
                short sample = (short) ((hi << 8) | lo);
                int bit = sample & 1;
                currentByte = (currentByte << 1) | bit;
                bitIndex++;
                if (bitIndex % 8 == 0) {
                    baos.write(currentByte);
                    String s = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    if (s.contains(DELIM)) break;
                    currentByte = 0;
                }
            }
            byte[] all = baos.toByteArray();
            String coll = new String(all, StandardCharsets.UTF_8);
            int delimIndex = coll.indexOf(DELIM);
            if (delimIndex == -1) {
                throw new Exception("No hidden message found or wrong key/audio file.");
            }
            return coll.substring(0, delimIndex);
        }
    }

    // Create a difference audio file (XOR LSBs of original and stego PCM data, keep header)
    public static void createLSBDifferenceWav(File originalWav, File stegoWav, File diffWav) throws Exception {
        try (DataInputStream origDis = new DataInputStream(new FileInputStream(originalWav));
             DataInputStream stegoDis = new DataInputStream(new FileInputStream(stegoWav));
             FileOutputStream diffFos = new FileOutputStream(diffWav)) {
            byte[] header = new byte[44];
            origDis.readFully(header);
            stegoDis.readFully(new byte[44]); // skip header in stego
            diffFos.write(header);
            byte[] origBuf = origDis.readAllBytes();
            byte[] stegoBuf = stegoDis.readAllBytes();
            int len = Math.min(origBuf.length, stegoBuf.length);
            for (int i = 0; i < len; i += 2) {
                short origSample = (short) ((origBuf[i+1] << 8) | (origBuf[i] & 0xFF));
                short stegoSample = (short) ((stegoBuf[i+1] << 8) | (stegoBuf[i] & 0xFF));
                short diffSample = (short) ((origSample & 1) ^ (stegoSample & 1));
                // Amplify the difference for audibility
                diffSample = (short) (diffSample == 1 ? 0x7FFF : 0x0000);
                diffFos.write(diffSample & 0xFF);
                diffFos.write((diffSample >> 8) & 0xFF);
            }
        }
    }
}