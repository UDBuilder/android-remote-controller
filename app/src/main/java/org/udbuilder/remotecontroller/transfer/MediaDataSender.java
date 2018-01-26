package org.udbuilder.remotecontroller.transfer;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.text.TextUtils;
import android.view.Surface;

import org.udbuilder.remotecontroller.utils.ByteArrayUtil;
import org.udbuilder.remotecontroller.utils.LogUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class MediaDataSender extends Transfer{
    private static final String TAG = "MediaDataSender";

    private final Thread mSender;
    private MediaProjection mMediaProjection;
    private MediaCodec mEncoder;
    private long mStartTime = 0;
    private Surface mSurface;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;

    private String mIp;

    private int mWidth;
    private int mHeight;
    private int mDpi;

    private EventReceiver mEventReceiver;

    public MediaDataSender(MediaProjection mp, String ip, TransferListener listener,
                           int width, int height, int dpi) {
        super(listener);
        mMediaProjection = mp;
        mIp = TextUtils.isEmpty(ip) ? Constant.IP_DEFAULT : ip;
//        mSender = new UDPSender();
        mSender = new TCPSender();
        mWidth = width;
        mHeight = height;
        mDpi = dpi;
    }

    private void prepareEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(Constant.MIME_TYPE,
                mWidth, mHeight);
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
                    mWidth, mHeight, mDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSurface, null, null);
            LogUtil.d(TAG, "created virtual display: " + mVirtualDisplay);
            mSender.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "start Exception: ", e);
        }
    }

    public void stop() {
        mQuit.set(true);
        if (mEventReceiver != null) {
            mEventReceiver.release();
        }
    }

    public void release() {
        stop();
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
//                            sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());
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
                                if (realData != null) {
                                    realData.position(mBufferInfo.offset);
                                    realData.limit(mBufferInfo.offset + mBufferInfo.size);
                                    sendRealData((mBufferInfo.presentationTimeUs / 1000) - mStartTime, realData);
                                } else {
                                    LogUtil.e(TAG, "run realData is null, index=" + eobIndex);
                                }
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
            byte[] data = new byte[length];
            realData.get(data, 0, length);
            ScreenData screenData = new ScreenData(data, (int) time);
            LogUtil.d(TAG, "sendRealData screenData=" + screenData);
            DatagramPacket packet = new DatagramPacket(screenData.getAllBytes(),
                    screenData.mSize, mInetAddr, Constant.PORT_SEND_DATA);
            try {
                mSocket.send(packet);
            } catch (Exception e) {
                LogUtil.e(TAG, "sendRealData Exception: ", e);
            }
        }

        private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
            byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
            int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                    AVCDecoderConfigurationRecord.length;
            byte[] finalBuff = new byte[packetLen];
            Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                    0,
                    true,
                    true,
                    AVCDecoderConfigurationRecord.length);
            System.arraycopy(AVCDecoderConfigurationRecord, 0,
                    finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
            ScreenData screenData = new ScreenData(finalBuff, (int) tms);
            LogUtil.d(TAG, "sendAVCDecoderConfigurationRecord screenData=" + screenData);
            DatagramPacket packet = new DatagramPacket(finalBuff,
                    finalBuff.length, mInetAddr, Constant.PORT_SEND_DATA);
            try {
                mSocket.send(packet);
            } catch (Exception e) {
                LogUtil.e(TAG, "sendAVCDecoderConfigurationRecord Exception: ", e);
            }
        }

    }

    private final class TCPSender extends Thread {

        private Socket mSocket = null;
        private OutputStream mOutputStream = null;

        TCPSender() {
            super("TCPSender");
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket(mIp, Constant.PORT_SEND_DATA);

                mEventReceiver = new EventReceiver(mSocket);
                mEventReceiver.start();

                mOutputStream = mSocket.getOutputStream();

                mListener.transferStart(null);

                // send width and height at first time
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt(mWidth));
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt(mHeight));
                mOutputStream.flush();

                while (!mQuit.get() && mSocket.isConnected()) {
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
//                            sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());
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
                            if (/*mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && */mBufferInfo.size != 0) {
                                ByteBuffer realData = mEncoder.getOutputBuffer(eobIndex);
                                if (realData != null) {
                                    realData.position(mBufferInfo.offset);
                                    realData.limit(mBufferInfo.offset + mBufferInfo.size);
                                    sendRealData((mBufferInfo.presentationTimeUs / 1000) - mStartTime, realData);
                                } else {
                                    LogUtil.e(TAG, "run realData is null, index=" + eobIndex);
                                }
                            }
                            mEncoder.releaseOutputBuffer(eobIndex, false);
                            break;
                    }
                }
                mListener.transferCompleted(null);
            } catch (Exception e) {
                LogUtil.e(TAG, "run Exception: ", e);
                mListener.transferError(null);
            } finally {
                if (mOutputStream != null) {
                    try {
                        mOutputStream.close();
                        mOutputStream = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mSocket != null) {
                    try {
                        mSocket.close();
                        mSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendRealData(long time, ByteBuffer realData) throws Exception {
            int length = realData.remaining();
            byte[] data = new byte[length];
            realData.get(data, 0, length);
            ScreenData screenData = new ScreenData(data, (int) time);
            LogUtil.d(TAG, "sendRealData screenData=" + screenData);
            try {
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt((int) time));
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt(length));
                mOutputStream.write(data);
                mOutputStream.flush();
            } catch (Exception e) {
                LogUtil.e(TAG, "sendRealData Exception: ", e);
            }
        }

        private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
            byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
            int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                    AVCDecoderConfigurationRecord.length;
            byte[] finalBuff = new byte[packetLen];
            Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                    0,
                    true,
                    true,
                    AVCDecoderConfigurationRecord.length);
            System.arraycopy(AVCDecoderConfigurationRecord, 0,
                    finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
            ScreenData screenData = new ScreenData(finalBuff, (int) tms);
            LogUtil.d(TAG, "sendAVCDecoderConfigurationRecord screenData=" + screenData);
            try {
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt((int) tms));
                mOutputStream.write(ByteArrayUtil.getByteArrayFromInt(packetLen));
                mOutputStream.write(finalBuff);
                mOutputStream.flush();
            } catch (Exception e) {
                LogUtil.e(TAG, "sendAVCDecoderConfigurationRecord Exception: ", e);
            }
        }
    }
}
