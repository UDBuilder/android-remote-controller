package org.udbuilder.remotecontroller.transfer;

import org.udbuilder.remotecontroller.utils.LogUtil;
import org.udbuilder.remotecontroller.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xiongjianbo on 2018/1/26.
 */

public class EventReceiver extends Thread {

    private static final String TAG = "EventReceiver";
    private static final int MAX_CAPACITY = 10;

    private Socket mSocket;
    private InputStream mInputStream;
    private final LinkedBlockingQueue<Event> mQueue;

    private AtomicBoolean mQuit = new AtomicBoolean(false);

    public EventReceiver(Socket socket) {
        mQueue = new LinkedBlockingQueue<>(MAX_CAPACITY);
        mSocket = socket;
        try {
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        mQuit.set(true);
        mQueue.clear();
    }

    @Override
    public void run() {
        while (!mQuit.get() && mSocket.isConnected()) {
            try {
                Event event = StreamUtils.readEvent(mInputStream);
                executeEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!mSocket.isConnected()) {
            LogUtil.w(TAG, "run socket isn't connected, quit!");
        }
    }

    private void executeEvent(Event event) {
        StringBuilder exe = new StringBuilder("input ");
        switch (event.getType()) {
            case Event.EVENT_TYPE_SWIPE:
                exe.append("swipe ");
                exe.append(event.getPointX());
                exe.append(" ");
                exe.append(event.getPointY());
                exe.append(" ");
                exe.append(event.getToPointX());
                exe.append(" ");
                exe.append(event.getToPointY());
                break;
            case Event.EVENT_TYPE_TAP:
                exe.append("tap ");
                exe.append(event.getPointX());
                exe.append(" ");
                exe.append(event.getPointY());
                break;
            default:
                LogUtil.d(TAG, "executeEvent unknown " + event);
                return;
        }
        exe.append("\n");
        LogUtil.d(TAG, "executeEvent exe=" + exe.toString());
        try {
            Process process = Runtime.getRuntime().exec(exe.toString());
//            process.getOutputStream().write(exe.toString().getBytes());
//            process.getOutputStream().flush();
        } catch (IOException e) {
            LogUtil.e(TAG, "executeEvent Exception: ", e);
        }
    }
}
