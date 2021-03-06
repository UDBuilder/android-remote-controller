package org.udbuilder.remotecontroller.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import org.udbuilder.remotecontroller.R;
import org.udbuilder.remotecontroller.transfer.Constant;
import org.udbuilder.remotecontroller.transfer.Event;
import org.udbuilder.remotecontroller.transfer.MediaDataReceiver;
import org.udbuilder.remotecontroller.transfer.TransferListener;
import org.udbuilder.remotecontroller.utils.LogUtil;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class ReceiverActivity extends AppCompatActivity implements TransferListener, TextureView.SurfaceTextureListener {

    private static final String TAG = "ReceiverActivity";

    private MediaDataReceiver mReceiver;
    private Surface mSurface;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_receiver);

        mTextureView = findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOnTouchListener(new EventCaptorTouchListener(this));
    }

    private static final int MSG_STARTED = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_COMPLETED = 3;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            LogUtil.d(TAG, "handleMessage " + getMessageString(msg.what));
            switch (msg.what) {
                case MSG_STARTED:
                    Bundle data = msg.getData();
                    int width = data.getInt(Constant.BUNDLE_WIDTH);
                    int height = data.getInt(Constant.BUNDLE_HEIGHT);
                    LogUtil.d(TAG, String.format("handleMessage width=%s, height=%s", width, height));
                    mSurfaceTexture.setDefaultBufferSize(width, height);
                    break;
                case MSG_ERROR:
                    break;
                case MSG_COMPLETED:
                    break;
            }
        }

        private String getMessageString(int what) {
            switch (what) {
                case MSG_STARTED:
                    return "MSG_STARTED";
                case MSG_ERROR:
                    return "MSG_ERROR";
                case MSG_COMPLETED:
                    return "MSG_COMPLETED";
            }
            return null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            mReceiver.release();
            mReceiver = null;
        }
    }

    @Override
    public void transferStart(Bundle bundle) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_STARTED;
        if (bundle != null) {
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }

    @Override
    public void transferError(Bundle bundle) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ERROR;
        if (bundle != null) {
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }

    @Override
    public void transferCompleted(Bundle bundle) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_COMPLETED;
        if (bundle != null) {
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        mSurfaceTexture = surface;
        LogUtil.d(TAG, String.format("onSurfaceTextureAvailable width=%s, height=%s", width, height));

        mReceiver = new MediaDataReceiver(mSurface, this);
        mReceiver.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        LogUtil.d(TAG, String.format("onSurfaceTextureSizeChanged width=%s, height=%s", width, height));
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        LogUtil.d(TAG, "onSurfaceTextureDestroyed ");
        if (mReceiver != null) {
            mReceiver.release();
            mReceiver = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        LogUtil.d(TAG, "onSurfaceTextureUpdated ");
    }

    private class EventCaptorTouchListener implements View.OnTouchListener {

        private int touchSlop;
        private GestureDetector mDetector;

        EventCaptorTouchListener(Context context) {
            touchSlop = ViewConfiguration.get(context.getApplicationContext()).getScaledTouchSlop();
            mDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public void onShowPress(MotionEvent e) {
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    Event event = new Event();
                    event.setType(Event.EVENT_TYPE_TAP);
                    event.setPointX((int) e.getX());
                    event.setPointY((int) e.getY());
                    LogUtil.d(TAG, "onSingleTapUp event=" + event);
                    mReceiver.sendEvent(event);
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    if (distanceX > touchSlop || distanceY > touchSlop) {
                        Event event = new Event();
                        event.setType(Event.EVENT_TYPE_TAP);
                        event.setPointX((int) e1.getX());
                        event.setPointY((int) e1.getY());
                        event.setToPointX((int) e2.getX());
                        event.setToPointY((int) e2.getY());
                        LogUtil.d(TAG, "onScroll event=" + event);
                    }
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    LogUtil.d(TAG, "onLongPress ");
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    LogUtil.d(TAG, "onFling ");
                    return false;
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.performClick();
            return mDetector.onTouchEvent(event);
        }


    }
}
