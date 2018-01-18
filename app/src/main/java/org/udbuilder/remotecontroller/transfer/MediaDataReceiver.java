package org.udbuilder.remotecontroller.transfer;

import org.udbuilder.remotecontroller.utils.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by asus on 2018/1/18.
 */

public class MediaDataReceiver {
    private static final String TAG = "MediaDataSender";

    private AtomicBoolean mQuit = new AtomicBoolean(false);

    private UDPReceiver mReceiver;

    public MediaDataReceiver() {
        mReceiver = new UDPReceiver();
    }

    public void start() {
        mReceiver.start();
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

                byte[] data = new byte[1000];
                DatagramPacket dp = new DatagramPacket(data, data.length);

                while(!mQuit.get()) {
                    try {
                        mSocket.receive(dp);
                        LogUtil.d(TAG, "run receive data length=" + dp.getLength() + " offset=" + dp.getOffset());
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

}
