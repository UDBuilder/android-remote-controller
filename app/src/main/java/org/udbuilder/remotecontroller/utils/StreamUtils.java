package org.udbuilder.remotecontroller.utils;

import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;

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
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e("readData Exception: ", "readData failed" + e);
        }
        if (readCount < length) {
            throw new Exception(String.format("can't read data enough, %s/%s", readCount, length));
        }
        return readCount;
    }
}
