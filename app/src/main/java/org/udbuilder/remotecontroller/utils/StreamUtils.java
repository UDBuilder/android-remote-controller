package org.udbuilder.remotecontroller.utils;

import android.os.SystemClock;

import org.udbuilder.remotecontroller.transfer.Event;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by xiongjianbo on 2018/1/25.
 */

public class StreamUtils {

    private static final String TAG = "StreamUtils";

    public static final int READ_DATA_TIMEOUT = 8000;

    public static int readData(InputStream inputStream, byte[] data, int length) throws Exception {
        int readCount = 0;

        Thread th = Thread.currentThread();

        if (th.isInterrupted() || (length <= 0)) {
            return readCount;
        }

        long beforeRead = SystemClock.currentThreadTimeMillis();

        try {
            while (readCount < length && !th.isInterrupted()) {
                long currentTime = SystemClock.currentThreadTimeMillis();

                if ((currentTime - beforeRead) > READ_DATA_TIMEOUT) {
                    LogUtil.e("utils: ", "readData timeout~");

                    break;
                }

                readCount += inputStream.read(data, readCount, length - readCount);

                if (readCount < 0) {
                    LogUtil.e("utils: ", "readCount is < 0, err~");
                }
            }
        } catch (IOException e) {
            LogUtil.e("readData IOException: ", "readData failed" + e);
        } catch (Exception e) {
            LogUtil.e("readData Exception: ", "readData failed" + e);
        }
        if (readCount < length) {
            throw new Exception(String.format("can't read data enough, %s/%s", readCount, length));
        }
        return readCount;
    }

    public static Event readEvent(InputStream inputStream) throws Exception {
        byte[] buff = new byte[2];
        Event event = new Event();
        readData(inputStream, buff, 1);
        event.setType(buff[0]);
        switch (event.getType()) {
            case Event.EVENT_TYPE_SWIPE:
                readData(inputStream, buff, 2);
                int x = ByteArrayUtil.twoByteToInt(buff, 0);
                readData(inputStream, buff, 2);
                int y = ByteArrayUtil.twoByteToInt(buff, 0);
                readData(inputStream, buff, 2);
                int toX = ByteArrayUtil.twoByteToInt(buff, 0);
                readData(inputStream, buff, 2);
                int toY = ByteArrayUtil.twoByteToInt(buff, 0);
                LogUtil.d(TAG, String.format("getEvent x=%s, y=%s, toX=%s, toY=%s", x, y, toX, toY));
                event.setPointX(x);
                event.setPointY(y);
                event.setPointX(toX);
                event.setPointY(toY);
                break;
            case Event.EVENT_TYPE_TAP:
                readData(inputStream, buff, 2);
                x = ByteArrayUtil.twoByteToInt(buff, 0);
                readData(inputStream, buff, 2);
                y = ByteArrayUtil.twoByteToInt(buff, 0);
                event.setPointX(x);
                event.setPointY(y);
                break;
        }
        LogUtil.d(TAG, "readEvent event=" + event);
        return event;
    }

    public static void sendEvent(OutputStream outputStream, Event event) throws IOException {
        LogUtil.d(TAG, "sendEvent event=" + event);
        byte[] buff = new byte[2];
        buff[0] = event.getType();
        outputStream.write(buff, 0, 1);
        switch (event.getType()) {
            case Event.EVENT_TYPE_SWIPE:
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getPointX());
                outputStream.write(buff);
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getPointY());
                outputStream.write(buff);
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getToPointX());
                outputStream.write(buff);
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getToPointY());
                outputStream.write(buff);
                break;
            case Event.EVENT_TYPE_TAP:
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getPointX());
                outputStream.write(buff);
                ByteArrayUtil.intToByteArrayTwoByte(buff, 0, event.getPointY());
                outputStream.write(buff);
                break;
        }
        outputStream.flush();
    }
}
