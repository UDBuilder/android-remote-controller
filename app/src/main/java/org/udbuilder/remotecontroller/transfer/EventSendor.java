package org.udbuilder.remotecontroller.transfer;

import org.udbuilder.remotecontroller.utils.LogUtil;
import org.udbuilder.remotecontroller.utils.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiongjianbo on 2018/1/26.
 */

public class EventSendor extends Thread {

    private static final String TAG = "EventSendor";
    private static final int MAX_CAPACITY = 10;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private final LinkedBlockingQueue<Event> mQueue;

    private AtomicBoolean mQuit = new AtomicBoolean(false);

    public EventSendor(Socket socket) {
        mQueue = new LinkedBlockingQueue<>(MAX_CAPACITY);
        mSocket = socket;
        try {
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        mQuit.set(true);
        mQueue.clear();
    }

    public void sendEvent(Event event) {
        try {
            mQueue.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(!mQuit.get() && mSocket.isConnected()) {
            Event event = mQueue.poll();
            if (event != null) {
                try {
                    StreamUtils.sendEvent(mOutputStream, event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!mSocket.isConnected()) {
            LogUtil.w(TAG, "run socket isn't connected, quit!");
        }
    }
}
