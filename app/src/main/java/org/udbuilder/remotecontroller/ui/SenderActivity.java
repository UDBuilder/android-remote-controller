package org.udbuilder.remotecontroller.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.udbuilder.remotecontroller.R;
import org.udbuilder.remotecontroller.transfer.MediaDataSender;
import org.udbuilder.remotecontroller.transfer.TransferListener;
import org.udbuilder.remotecontroller.utils.LogUtil;

/**
 * Created by xiongjianbo on 2018/1/18.
 */

public class SenderActivity extends AppCompatActivity implements TransferListener {

    private static final String TAG = "SenderActivity";

    private MediaProjectionManager mMediaProjectionMgr;
    private MediaDataSender mSender;
    private boolean mRunning = false;
    private Button mBtn;
    private EditText mText;

    private int mWidth;
    private int mHeight;
    private int mDpi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        mBtn = (Button) findViewById(R.id.btn_execute);
        mText = findViewById(R.id.ip);

        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        mDpi = /*dm.densityDpi*/1;
        mWidth = dm.widthPixels / 2;
        mHeight = dm.heightPixels / 2;
    }


    public void onActionExecute(View view) {
        mRunning = !mRunning;
        if (mRunning) {
            if (mMediaProjectionMgr == null) {
                mMediaProjectionMgr = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            }
            if (mMediaProjectionMgr != null) {
                startActivityForResult(mMediaProjectionMgr.createScreenCaptureIntent(), 1);
            } else {
                LogUtil.e(TAG, "onActionExecute mMediaProjectionMgr is null");
            }
            mBtn.setText("Stop");
        } else {
            if (mSender != null) {
                mSender.stop();
            }
            mBtn.setText("Start");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            LogUtil.e(TAG, "onActivityResult permission denied");
            return;
        }
        if (mSender == null) {
            MediaProjection mp = mMediaProjectionMgr.getMediaProjection(resultCode, data);
            mSender = new MediaDataSender(mp, mText.getText().toString(), this, mWidth, mHeight, mDpi);
        }
        mSender.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSender != null) {
            mSender.release();
        }
    }

    @Override
    public void transferStart(Bundle bundle) {

    }

    @Override
    public void transferError(Bundle bundle) {

    }

    @Override
    public void transferCompleted(Bundle bundle) {

    }
}
