package org.udbuilder.remotecontroller.ui;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import org.udbuilder.remotecontroller.R;
import org.udbuilder.remotecontroller.transfer.Constant;
import org.udbuilder.remotecontroller.transfer.MediaDataReceiver;
import org.udbuilder.remotecontroller.transfer.TransferListener;
import org.udbuilder.remotecontroller.utils.LogUtil;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class ReceiverActivity extends AppCompatActivity implements TransferListener, TextureView.SurfaceTextureListener {

    private static final String TAG = "ReceiverActivity";

    private Button mBtn;
    private boolean mRunning = false;
    private MediaDataReceiver mReceiver;
    private Surface mSurface;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        mBtn = (Button) findViewById(R.id.btn_execute);
        mTextureView = findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(this);
    }

    public void onActionExecute(View view) {
        mRunning = !mRunning;
        if (mRunning) {
            mBtn.setText("Stop");
            if (mReceiver == null) {
                mReceiver = new MediaDataReceiver(mSurface, this);
            }
            mReceiver.start();
        } else {
            mBtn.setText("Start");
            if (mReceiver != null) {
                mReceiver.stop();
            }
        }
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
                    mBtn.setVisibility(View.GONE);
                    break;
                case MSG_ERROR:
                    mBtn.setVisibility(View.VISIBLE);
                    break;
                case MSG_COMPLETED:
                    mBtn.setVisibility(View.VISIBLE);
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
}
