package org.udbuilder.remotecontroller.transfer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;

import org.udbuilder.remotecontroller.utils.ByteArrayUtil;
import org.udbuilder.remotecontroller.utils.LogUtil;
import org.udbuilder.remotecontroller.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by asus on 2018/1/18.
 */

public class MediaDataReceiver extends Transfer {
    private static final String TAG = "MediaDataReceiver";

    private AtomicBoolean mQuit = new AtomicBoolean(false);

    private Thread mReceiver;
    private Surface mSurface;
    private MediaCodec mDecoder;
    MediaCodec.BufferInfo mBuffInfo = new MediaCodec.BufferInfo();
    private ByteBuffer mCSD0;
    private ByteBuffer mCSD1;

    private int mWidth;
    private int mHeight;

    public MediaDataReceiver(Surface surface, TransferListener listener) {
        super(listener);
        mSurface = surface;
//        mReceiver = new UDPReceiver();
        mReceiver = new TCPReceiver();
    }

    public void start() {
        mReceiver.start();
    }

    private static int getXPS(byte[] data, int offset, int length, byte[] dataOut, int[] outLen, int type) {
        int i;
        int pos0;
        int pos1;
        pos0 = -1;
        length = Math.min(length, data.length);
        for (i = offset; i < length - 4; i++) {
            if ((0 == data[i]) && (0 == data[i + 1]) && (1 == data[i + 2]) && (type == (0x0F & data[i + 3]))) {
                pos0 = i;
                break;
            }
        }
        if (-1 == pos0) {
            return -1;
        }
        if (pos0 > 0 && data[pos0 - 1] == 0) { // 0 0 0 1
            pos0 = pos0 - 1;
        }
        pos1 = -1;
        for (i = pos0 + 4; i < length - 4; i++) {
            if ((0 == data[i]) && (0 == data[i + 1]) && (1 == data[i + 2])) {
                pos1 = i;
                break;
            }
        }
        if (-1 == pos1 || pos1 == 0) {
            return -2;
        }
        if (data[pos1 - 1] == 0) {
            pos1 -= 1;
        }
        if (pos1 - pos0 > outLen[0]) {
            return -3; // 输入缓冲区太小
        }
        dataOut[0] = 0;
        System.arraycopy(data, pos0, dataOut, 0, pos1 - pos0);
        // memcpy(pXPS+1, pES+pos0, pos1-pos0);
        // *pMaxXPSLen = pos1-pos0+1;
        outLen[0] = pos1 - pos0;
        return pos1;
    }

    private static long fixSleepTime(long sleepTimeUs, long totalTimestampDifferUs, long delayUs) {
        if (totalTimestampDifferUs < 0L) {
            LogUtil.w(TAG, String.format("totalTimestampDifferUs is:%d, this should not be happen.", totalTimestampDifferUs));
            totalTimestampDifferUs = 0;
        }
        double dValue = ((double) (delayUs - totalTimestampDifferUs)) / 1000000d;
        double radio = Math.pow(30, dValue);
        final double r = sleepTimeUs * radio + 0.5f;
//        Log.d(TAG,String.format("fixSleepTime %d,%d,%d->%d", sleepTimeUs, totalTimestampDifferUs, delayUs, (long) r));
        return (long) r;
    }

    private void prepareDecoder() throws IOException {
        if (mDecoder == null) {
            MediaFormat format = MediaFormat.createVideoFormat(Constant.MIME_TYPE,
                    mWidth, mHeight);
//            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//            format.setInteger(MediaFormat.KEY_BIT_RATE, Constant.VIDEO_BITRATE);
//            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constant.FRAME_RATE);
//            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constant.IFRAME_INTERVAL);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);
            if (mCSD0 != null) {
                format.setByteBuffer("csd-0", mCSD0);
            } else {
                LogUtil.w(TAG, "prepareDecoder invalid mCSD0");
            }
            if (mCSD1 != null) {
                format.setByteBuffer("csd-1", mCSD1);
            } else {
                LogUtil.w(TAG, "prepareDecoder invalid mCSD1");
            }

            LogUtil.d(TAG, "prepareDecoder video format: " + format);
            mDecoder = MediaCodec.createDecoderByType(Constant.MIME_TYPE);
            mDecoder.configure(format, mSurface, null, 0);
            mDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mDecoder.start();
        }
    }


    public void stop() {
        mQuit.set(true);
    }

    public void release() {
        stop();
        if (mDecoder != null) {
            mDecoder.release();
        }
    }


    private final class UDPReceiver extends Thread {
        private DatagramSocket mSocket;

        public UDPReceiver() {
            super("UDPReceiver");
        }

        @Override
        public void run() {
            try {
                // 根据主机名称得到IP地址
                InetSocketAddress address = new InetSocketAddress(Constant.IP_DEFAULT, Constant.PORT_SEND_DATA);
                // 创建数据报文套接字并通过它传送
                mSocket = new DatagramSocket(address);
                mSocket.setSoTimeout(3000);

                byte[] data = new byte[10000];
                DatagramPacket dp = new DatagramPacket(data, data.length);

                int index = 0;
                long previewStampUs = 0L;
                boolean mm;
                ScreenData screenData = new ScreenData();

                boolean mWaitingKeyFrame = true;
                while (!mQuit.get()) {
                    try {
                        mSocket.receive(dp);
                        screenData.setBytes(dp.getData(), dp.getOffset(), dp.getLength());
                        LogUtil.d(TAG, "run receive screenData=" + screenData);

                        if (mWaitingKeyFrame) {
//                            byte[] key = screenData.getKeyFrame();
//                            if (key != null) {
//                                mCSD0 = ByteBuffer.wrap(key);
//                                LogUtil.d(TAG, "run find CSD0");
//                                mWaitingKeyFrame = false;
//                            }
                            byte[] dataOut = new byte[128];
                            int[] outLen = new int[]{128};
                            int result = getXPS(screenData.getBuffer(), 0, 256, dataOut, outLen, 7);
                            if (result >= 0) {
                                ByteBuffer csd0 = ByteBuffer.allocate(outLen[0]);
                                csd0.put(dataOut, 0, outLen[0]);
                                csd0.clear();
                                mCSD0 = csd0;
                                LogUtil.i(TAG, String.format("CSD-0 searched"));
                            }
                            outLen[0] = 128;
                            result = getXPS(screenData.getBuffer(), 0, 256, dataOut, outLen, 8);
                            if (result >= 0) {
                                ByteBuffer csd1 = ByteBuffer.allocate(outLen[0]);
                                csd1.put(dataOut, 0, outLen[0]);
                                csd1.clear();
                                mCSD1 = csd1;
                                LogUtil.i(TAG, String.format("CSD-1 searched"));
                            }
                            mWaitingKeyFrame = false;
                        }

                        prepareDecoder();
                        mm = true;
                        do {
                            if (mm && dp.getLength() > 0) {
                                byte[] pBuf = screenData.mBuffer;
                                index = mDecoder.dequeueInputBuffer(10);
                                if (index >= 0) {
                                    ByteBuffer buffer = mDecoder.getInputBuffer(index);
                                    buffer.clear();
                                    int stamp = screenData.mTimeStamp;
                                    if (pBuf.length > buffer.remaining()) {
                                        mDecoder.queueInputBuffer(index, 0, 0, stamp, 0);
                                    } else {
                                        buffer.put(pBuf, 0, pBuf.length);
                                        mDecoder.queueInputBuffer(index, 0, buffer.position(), stamp, 0);
                                    }
                                    mm = false;
                                }
                            }

                            index = mDecoder.dequeueOutputBuffer(mBuffInfo, 10); //
                            switch (index) {
                                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                    LogUtil.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                                    break;
                                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                    MediaFormat mf = mDecoder.getOutputFormat();
                                    LogUtil.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED ：" + mf);
                                    break;
                                case MediaCodec.INFO_TRY_AGAIN_LATER:
                                    LogUtil.i(TAG, "INFO_TRY_AGAIN_LATER");
                                    // 输出为空
                                    break;
                                default:
                                    long newSleepUs = -1;
                                    boolean firstTime = previewStampUs == 0L;
                                    if (!firstTime) {
                                        long sleepUs = (mBuffInfo.presentationTimeUs - previewStampUs);
                                        if (sleepUs > 50000) {
                                            // 时间戳异常，可能服务器丢帧了。
                                            LogUtil.w(TAG, "sleep time.too long:" + sleepUs);
                                            sleepUs = 50000;
                                        }
                                        {
                                            long cache = screenData.mTimeStamp - previewStampUs;
                                            newSleepUs = fixSleepTime(sleepUs, cache, -100000);
                                            // Log.d(TAG, String.format("sleepUs:%d,newSleepUs:%d,Cache:%d", sleepUs, newSleepUs, cache));
                                            LogUtil.d(TAG, "cache:" + cache);
                                        }
                                    }
                                    previewStampUs = mBuffInfo.presentationTimeUs;

                                    if (newSleepUs > 0) {
                                        LogUtil.i(TAG, String.format("sleep:%d", newSleepUs / 1000));
                                        Thread.sleep(newSleepUs / 1000);
                                    }
                                    mDecoder.releaseOutputBuffer(index, true);
                                    if (firstTime) {
                                        LogUtil.i(TAG, String.format("POST VIDEO_DISPLAYED!!!"));
                                    }
                            }
                        } while (index < MediaCodec.INFO_TRY_AGAIN_LATER);
                    } catch (Exception e) {
                        LogUtil.e(TAG, "run Exception: ", e);
                    }
                }

            } catch (Exception e) {
                LogUtil.e(TAG, "run Exception: ", e);
            } finally {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
            }
        }
    }

    private final class TCPReceiver extends Thread {

        TCPReceiver() {
            super("TCPReceiver");
        }

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            Socket socket = null;
            InputStream inputStream = null;
            try {
                serverSocket = new ServerSocket(Constant.PORT_SEND_DATA);
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                byte[] buffers = new byte[50000];
                int stamp;
                int length;
                boolean mWaitingKeyFrame = true;
                boolean mm;
                int index;
                long previewStampUs = 0L;

                // read width and height at first time
                StreamUtils.readData(inputStream, buffers, 4);
                mWidth = ByteArrayUtil.getIntFromByteArray(buffers, 0);
                StreamUtils.readData(inputStream, buffers, 4);
                mHeight = ByteArrayUtil.getIntFromByteArray(buffers, 0);
                LogUtil.d(TAG, String.format("screen width:%s, height:%s", mWidth, mHeight));

                Bundle bundle = new Bundle();
                bundle.putInt(Constant.BUNDLE_WIDTH, mWidth);
                bundle.putInt(Constant.BUNDLE_HEIGHT, mHeight);
                mListener.transferStart(bundle);

                while (!mQuit.get() && socket.isConnected()) {

                    // read timestamp
                    StreamUtils.readData(inputStream, buffers, 4);
                    stamp = ByteArrayUtil.getIntFromByteArray(buffers, 0);

                    // read length of frame
                    StreamUtils.readData(inputStream, buffers, 4);
                    length = ByteArrayUtil.getIntFromByteArray(buffers, 0);

                    LogUtil.d(TAG, String.format("stamp:%s, length:%s", stamp, length));

                    // read frame
                    if (length > buffers.length) {
                        buffers = new byte[length];
                    }
                    int len = StreamUtils.readData(inputStream, buffers, length);
                    if (len != length) {
                        throw new Exception(String.format("unexpected data length," +
                                " expect:%s, but actually:%s", length, len));
                    }

                    // wait key frame to get CSD-0
                    if (mWaitingKeyFrame) {
                        byte[] dataOut = new byte[128];
                        int[] outLen = new int[]{128};
                        int result = getXPS(buffers, 0, 256, dataOut, outLen, 7);
                        if (result >= 0) {
                            ByteBuffer csd0 = ByteBuffer.allocate(outLen[0]);
                            csd0.put(dataOut, 0, outLen[0]);
                            csd0.clear();
                            mCSD0 = csd0;
                            LogUtil.i(TAG, "CSD-0 searched");
                        }
                        outLen[0] = 128;
                        result = getXPS(buffers, 0, 256, dataOut, outLen, 8);
                        if (result >= 0) {
                            ByteBuffer csd1 = ByteBuffer.allocate(outLen[0]);
                            csd1.put(dataOut, 0, outLen[0]);
                            csd1.clear();
                            mCSD1 = csd1;
                            LogUtil.i(TAG, "CSD-1 searched");
                        }
                        mWaitingKeyFrame = false;
                        // init decoder
                        prepareDecoder();
                    }

                    if (mDecoder == null) {
                        LogUtil.w(TAG, "Decoder hasn't been initialized");
                        continue;
                    }

                    mm = true;
                    do {
                        if (mm && length > 0) {
                            index = mDecoder.dequeueInputBuffer(10);
                            if (index >= 0) {
                                ByteBuffer buffer = mDecoder.getInputBuffer(index);
                                buffer.clear();
                                if (length > buffer.remaining()) {
                                    mDecoder.queueInputBuffer(index, 0, 0, stamp, 0);
                                } else {
                                    buffer.put(buffers, 0, length);
                                    mDecoder.queueInputBuffer(index, 0, buffer.position(), stamp, 0);
                                }
                                mm = false;
                            }
                        }

                        index = mDecoder.dequeueOutputBuffer(mBuffInfo, 10); //
                        switch (index) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                LogUtil.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                MediaFormat mf = mDecoder.getOutputFormat();
                                LogUtil.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED ：" + mf);
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                LogUtil.i(TAG, "INFO_TRY_AGAIN_LATER");
                                // 输出为空
                                break;
                            default:
                                long newSleepUs = -1;
                                boolean firstTime = previewStampUs == 0L;
                                if (!firstTime) {
                                    long sleepUs = (mBuffInfo.presentationTimeUs - previewStampUs);
                                    if (sleepUs > 50000) {
                                        // 时间戳异常，可能服务器丢帧了。
                                        LogUtil.w(TAG, "sleep time.too long:" + sleepUs);
                                        sleepUs = 50000;
                                    }
                                    {
                                        long cache = stamp - previewStampUs;
                                        newSleepUs = fixSleepTime(sleepUs, cache, -100000);
                                        // Log.d(TAG, String.format("sleepUs:%d,newSleepUs:%d,Cache:%d", sleepUs, newSleepUs, cache));
                                        LogUtil.d(TAG, "cache:" + cache);
                                    }
                                }
                                previewStampUs = mBuffInfo.presentationTimeUs;

                                if (newSleepUs >= 1000) {
                                    LogUtil.i(TAG, String.format("sleep:%s", newSleepUs / 1000));
                                    Thread.sleep(newSleepUs / 1000);
                                }
                                mDecoder.releaseOutputBuffer(index, true);
                                if (firstTime) {
                                    LogUtil.i(TAG, "POST VIDEO_DISPLAYED!!!");
                                }
                        }
                    } while (index < MediaCodec.INFO_TRY_AGAIN_LATER);
                }
                mListener.transferCompleted(null);
            } catch (Exception e) {
                LogUtil.e(TAG, "run Exception: ", e);
                mListener.transferError(null);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        LogUtil.e(TAG, "run close Exception: ", e);
                    }
                }
            }
        }
    }
}
