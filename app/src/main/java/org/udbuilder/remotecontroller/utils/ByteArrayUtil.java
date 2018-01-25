package org.udbuilder.remotecontroller.utils;

/**
 * Created by lake on 16-3-30.
 * Big-endian
 */
public class ByteArrayUtil {
    public static void intToByteArrayFull(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) ((interger >> 24) & 0xFF);
        dst[pos + 1] = (byte) ((interger >> 16) & 0xFF);
        dst[pos + 2] = (byte) ((interger >> 8) & 0xFF);
        dst[pos + 3] = (byte) ((interger) & 0xFF);
    }

    public static byte[] getByteArrayFromInt(int interger) {
        byte[] dst = new byte[4];
        dst[0] = (byte) ((interger >> 24) & 0xFF);
        dst[1] = (byte) ((interger >> 16) & 0xFF);
        dst[2] = (byte) ((interger >> 8) & 0xFF);
        dst[3] = (byte) ((interger) & 0xFF);
        return dst;
    }

    public static int getIntFromByteArray(byte[] dst, int offset) {
        int value= 0;
        //由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift= (4 - 1 - i) * 8;
            value +=(dst[i + offset] & 0x000000FF) << shift;//往高位游
        }
        return value;
//        int b0 = dst[offset] & 0xFF;
//        int b1 = dst[offset + 1] & 0xFF;
//        int b2 = dst[offset + 2] & 0xFF;
//        int b3 = dst[offset + 3] & 0xFF;
//        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    public static void intToByteArrayTwoByte(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) ((interger >> 8) & 0xFF);
        dst[pos + 1] = (byte) ((interger) & 0xFF);
    }
}
