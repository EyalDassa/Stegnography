package com.example.stegnography.stego.audio;

import com.example.stegnography.crypto.CryptoUtils;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import javax.crypto.SecretKey;

public class LSBAudioSteganography {
    private static final String DELIM = "<<<END>>>";

    public void embed(File inWav, File outWav, String message, SecretKey key) throws Exception {
        // read entire WAV
        byte[] header = new byte[44];
        try (DataInputStream dis = new DataInputStream(new FileInputStream(inWav))) {
            dis.readFully(header);
            byte[] pcm = dis.readAllBytes();

            // encrypt + delimiter
            String enc = CryptoUtils.encrypt(message, key) + DELIM;
            byte[] data = enc.getBytes("UTF-8");
            BitSet bits = BitSet.valueOf(data);

            // modify LSB of each sample (16-bit little endian)
            ByteBuffer bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < pcm.length/2 && i*8 < data.length*8; i++) {
                short sample = bb.getShort(i*2);
                int bit = bits.get(i) ? 1 : 0;
                sample = (short)((sample & ~1) | bit);
                bb.putShort(i*2, sample);
            }

            try (FileOutputStream fos = new FileOutputStream(outWav)) {
                fos.write(header);
                fos.write(bb.array());
            }
        }
    }

    public String extract(File inWav, SecretKey key) throws Exception {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(inWav))) {
            dis.skipBytes(44);
            byte[] pcm = dis.readAllBytes();
            BitSet bits = new BitSet();
            ByteBuffer bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < pcm.length/2; i++) {
                short sample = bb.getShort(i*2);
                bits.set(i, (sample & 1) == 1);
                // every byte, check delim
                if ((i+1)%8 == 0) {
                    byte[] soFar = bits.toByteArray();
                    String s = new String(soFar, "UTF-8");
                    if (s.contains(DELIM)) break;
                }
            }

            byte[] all = bits.toByteArray();
            String coll = new String(all, "UTF-8");
            String encMsg = coll.substring(0, coll.indexOf(DELIM));
            return CryptoUtils.decrypt(encMsg, key);
        }
    }
}