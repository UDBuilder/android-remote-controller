package org.udbuilder.remotecontroller.transfer;

import org.udbuilder.remotecontroller.utils.ByteArrayUtil;

import java.util.Arrays;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class ScreenData {

    public int mTimeStamp;//解码时间戳

    public byte[] mBuffer; //数据

    public int mSize; //字节长度

    public ScreenData() {
    }

    public ScreenData(byte[] buffer, int stamp) {
        mBuffer = buffer;
        mTimeStamp = stamp;
        mSize = mBuffer.length + 8;
    }

    public byte[] getAllBytes() {
        int len = mBuffer.length + 8;
        byte[] ret = new byte[len];
        ByteArrayUtil.intToByteArrayFull(ret, 0, mTimeStamp);
        ByteArrayUtil.intToByteArrayFull(ret, 4, mSize);
        System.arraycopy(mBuffer, 0, ret, 8, mBuffer.length);
        return ret;
    }

    public byte[] getBuffer() {
        return mBuffer;
    }

    public void set(byte[] bytes, int offset, int len, int stamp) {
        mBuffer = new byte[len];
        System.arraycopy(bytes, offset, mBuffer, 0, mBuffer.length);
        mSize = len;
        mTimeStamp = stamp;
    }

    public void setBytes(byte[] bytes, int offset, int len) {
        if (bytes == null || len <= 8) {
            throw new RuntimeException("invalid bytes");
        }
        mTimeStamp = ByteArrayUtil.getIntFromByteArray(bytes, offset);
        mSize = ByteArrayUtil.getIntFromByteArray(bytes, offset + 4);
        mBuffer = new byte[len - 8];
        System.arraycopy(bytes, offset + 8, mBuffer, 0, mBuffer.length);
    }

    public byte[] getKeyFrame() {
        //找到sps与pps的分隔处
        int pos = 0;
        if (!((pos + 3 < mBuffer.length) && (mBuffer[pos] == 0 && mBuffer[pos + 1] == 0 && mBuffer[pos + 2] == 0 && mBuffer[pos + 3] == 1))) {
            return null;
        } else {
            //00 00 00 01开始标志后的一位
            pos = 4;
        }
//        while ((pos + 3 < dp.getLength()) && !(data[pos] == 0 && data[pos + 1] == 0 && data[pos + 2] == 0 && data[pos + 3] == 1)) {
//            pos++;
//        }
//        if (pos + 3 >= dp.getLength()) {
//            return null;
//        }
        return Arrays.copyOfRange(mBuffer, pos, mBuffer.length);
    }

    @Override
    public String toString() {
        return String.format("ScreenData[stamp:%s, size:%s, buffer.length:%s]",
                mTimeStamp, mSize, mBuffer == null ? 0 : mBuffer.length);
    }
}
