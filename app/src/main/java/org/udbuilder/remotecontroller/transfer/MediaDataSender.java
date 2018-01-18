package org.udbuilder.remotecontroller.transfer;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.text.TextUtils;
import android.view.Surface;

import org.udbuilder.remotecontroller.utils.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class MediaDataSender {
    private static final String TAG = "MediaDataSender";

    private final UDPSender mSender;
    private MediaProjection mMediaProjection;
    private MediaCodec mEncoder;
    private long mStartTime = 0;
    private Surface mSurface;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;

    private String mIp;

    public MediaDataSender(MediaProjection mp, String ip) {
        mMediaProjection = mp;
        mIp = TextUtils.isEmpty(ip) ? Constant.IP_DEFAULT : ip;
        mSender = new UDPSender();
    }

    private void prepareEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(Constant.MIME_TYPE,
                Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, Constant.VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, Constant.FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constant.IFRAME_INTERVAL);
        LogUtil.d(TAG, "created video format: " + format);
        mEncoder = MediaCodec.createEncoderByType(Constant.MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
        LogUtil.d(TAG, "created input surface: " + mSurface);
        mEncoder.start();
    }

    public void start() {
        try {
            prepareEncoder();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT, Constant.VIDEO_DPI,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mSurface, null, null);
            LogUtil.d(TAG, "created virtual display: " + mVirtualDisplay);
            mSender.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "start Exception: ", e);
        }
    }

    public void stop() {
        mQuit.set(true);
    }

    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }


    private final class UDPSender extends Thread {
        private LinkedBlockingDeque<ScreenData> mFrameQueue;
        private DatagramSocket mSocket;
        private InetAddress mInetAddr;

        UDPSender() {
            super("UDPSender");
            mFrameQueue = new LinkedBlockingDeque<>(Constant.MAX_QUEUE_CAPACITY);
        }

        @Override
        public void run() {

            try {
                // 根据主机名称得到IP地址
                mInetAddr = InetAddress.getByName(mIp);
                // 创建数据报文套接字并通过它传送
                mSocket = new DatagramSocket();
                mSocket.setSoTimeout(3000);

                while (!mQuit.get()) {
                    int eobIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, Constant.TIMEOUT_US);
                    switch (eobIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            LogUtil.d(TAG, "VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            LogUtil.d(TAG, "VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            LogUtil.d(TAG, "VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                                    mEncoder.getOutputFormat().toString());
                            break;
                        default:
                            LogUtil.d(TAG, "VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                            if (mStartTime == 0) {
                                mStartTime = mBufferInfo.presentationTimeUs / 1000;
                            }
                            /**
                             * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                             * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                             */
                            if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                                ByteBuffer realData = mEncoder.getOutputBuffer(eobIndex);
                                realData.position(mBufferInfo.offset + 4);
                                realData.limit(mBufferInfo.offset + mBufferInfo.size);
                                sendRealData((mBufferInfo.presentationTimeUs / 1000) - mStartTime, realData);
                            }
                            mEncoder.releaseOutputBuffer(eobIndex, false);
                            break;
                    }
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "run Exception: ", e);
            } finally {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                mFrameQueue.clear();
            }
        }

        private void sendRealData(long time, ByteBuffer realData) {
            int length = realData.remaining();
            LogUtil.d(TAG, "sendRealData length=" + length + " time=" + time);
            byte[] data = new byte[length];
            realData.get(data, 0, length);
            DatagramPacket packet = new DatagramPacket(data, data.length, mInetAddr, Constant.PORT_SEND_DATA);
            try {
                mSocket.send(packet);
            } catch (Exception e) {
                LogUtil.e(TAG, "sendRealData Exception: ", e);
            }
        }

    }
}
